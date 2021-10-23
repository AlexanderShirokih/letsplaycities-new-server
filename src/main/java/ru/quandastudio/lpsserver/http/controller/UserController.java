package ru.quandastudio.lpsserver.http.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.quandastudio.lpsserver.Result;
import ru.quandastudio.lpsserver.core.game.Player;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.PictureManager;
import ru.quandastudio.lpsserver.data.entities.Picture;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.http.model.MessageWrapper;
import ru.quandastudio.lpsserver.http.model.SignUpRequest;
import ru.quandastudio.lpsserver.http.model.SignUpResponse;
import ru.quandastudio.lpsserver.models.FriendModeResult;
import ru.quandastudio.lpsserver.models.FriendshipStatus;
import ru.quandastudio.lpsserver.models.LPSMessage.LPSFriendModeRequest;
import ru.quandastudio.lpsserver.models.ProfileInfo;
import ru.quandastudio.lpsserver.models.RequestType;
import ru.quandastudio.lpsserver.util.ValidationUtil;

import java.io.IOException;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
@Slf4j
public class UserController {

    private final ServerContext context;

    @RequestMapping(value = "/{id}/picture", produces = {MediaType.TEXT_PLAIN_VALUE, "image/*"})
    public ResponseEntity<ByteArrayResource> getPicture(@PathVariable(value = "id") String userId) {
        return Result.of(() -> new User(Integer.parseInt(userId)))
                .map((User user) -> context.getPictureManager().getPictureByUserId(user))
                .flatMap((Optional<Picture> picture) -> Result.from(picture, "No picture"))
                .map((Picture p) -> {
                    boolean isBase64 = p.getType() == Picture.Type.BASE64;
                    ByteArrayResource res = new ByteArrayResource(isBase64 ? decodeBase64(p.getImageData()) : p.getImageData());
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(isBase64 ? Picture.Type.PNG.getMediaType() : p.getType().getMediaType()))
                            .contentLength(res.contentLength())
                            .body(res);
                })
                .getOr(ResponseEntity.notFound().build());
    }

    private static byte[] decodeBase64(byte[] data) {
        return Base64.getDecoder().decode(data);
    }

    private static final List<String> SUPPORTED_TYPES = Arrays.asList("base64", "png", "jpeg", "gif");

    // Deprecated since 1.4.8
    @PostMapping("/picture")
    public ResponseEntity<MessageWrapper<String>> updatePicture(@AuthenticationPrincipal User user,
                                                                @RequestParam("t") String type, @RequestParam("hash") String hash, @RequestBody byte[] body) {
        return Result.success("ok")
                .check(() -> hash.length() == 32, "Invalid hash code")
                .check(() -> SUPPORTED_TYPES.contains(type), "Unsupported image type")
                .check(() -> body.length < 64 * 1024, "Image is too big")
                .apply((String imageData) -> {
                    final Picture.Type t = Picture.Type.valueOf(type.toUpperCase());
                    updatePicture(user, t, hash, body);
                })
                .wrap()
                .toResponse();
    }

    @PostMapping("/picture/upload")
    public ResponseEntity<MessageWrapper<String>> updatePicture(@AuthenticationPrincipal User user, @RequestParam("imageFile") MultipartFile file, @RequestParam("hash") String hash) {
        return Result.success("ok")
                .check(() -> hash.length() == 32, "Invalid hash code")
                .check(() -> file.getSize() < 64 * 1024, "Image is too big")
                .flatMap((String imageData) -> {
                    final Picture.Type t = Picture.Type.fromMimeType(file.getContentType());
                    if (t == null)
                        return Result.error("Unsupported image type: " + file.getContentType());

                    try {
                        updatePicture(user, t, hash, file.getBytes());
                        return Result.success("ok");
                    } catch (IOException e) {
                        return Result.error(e);
                    }
                })
                .wrap()
                .toResponse();
    }

    private void updatePicture(User user, Picture.Type t, String hash, byte[] body) {
        PictureManager pics = context.getPictureManager();
        if (!Objects.equals(hash, user.getAvatarHash())) {
            var response = pics.getPictureByUserId(user);
            Picture pic = response.orElse(new Picture(user, body, t));
            pic.setImageData(body);
            pic.setType(t);
            pics.save(pic);
            context.getUserManager().updateAvatarHash(user, hash);
        }
    }

    @DeleteMapping("/picture")
    @ResponseStatus(HttpStatus.OK)
    public void deletePicture(@AuthenticationPrincipal User user) {
        context.getPictureManager().deletePictureByUser(user);
        context.getUserManager().updateAvatarHash(user, null);
    }

    @PostMapping("/")
    public ResponseEntity<MessageWrapper<SignUpResponse>> signUp(@RequestBody SignUpRequest request) {
        final Optional<String> firstError = ValidationUtil.validateMessage(request);

        return Result.success(request)
                .check(firstError::isEmpty, firstError.orElse(null))
                .flatMap(context.getUserManager()::logIn)
                .map(SignUpResponse::new)
                .wrap()
                .toOkResponse();
    }

    /**
     * Shows info about user profile.
     * Requires authorization.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProfileInfo> showProfile(@PathVariable("id") int userId, @AuthenticationPrincipal User user) {
        return ResponseEntity.of(
                userId == user.getId()
                        ? context.getUserManager().getUserProfileById(userId)
                        : context.getUserManager().getUserProfileByIdFromSpectator(userId, user.getId()
                )
        );
    }

    @PostMapping("/token/{token}")
    @ResponseStatus(HttpStatus.OK)
    public void updateToken(@AuthenticationPrincipal User user, @PathVariable("token") String token) {
        context.getUserManager().updateFirebaseToken(user, token);
    }

    @PutMapping("/request/{id}/{type}")
    public ResponseEntity<String> handleGameRequest(@PathVariable("id") int userId,
                                                    @PathVariable("type") RequestType requestType, @AuthenticationPrincipal User user) {
        if (requestType == RequestType.DENY) {
            handleDenyResult(user, new User(userId));
            return ResponseEntity.ok("ok");
        }
        return ResponseEntity.badRequest().build();
    }

    private void handleDenyResult(User current, User oppUser) {
        final Optional<Player> sender = context.popPlayerFromWaitingQueue(current, oppUser);

        sender.ifPresent((Player player) -> {
            if (context.getFriendshipManager().getFriendshipStatus(current, oppUser) == FriendshipStatus.friends) {
                player.sendMessage(new LPSFriendModeRequest(current, FriendModeResult.DENIED));
            } else {
                player.sendMessage(new LPSFriendModeRequest(current, FriendModeResult.NOT_FRIEND));
            }
        });
    }


}
