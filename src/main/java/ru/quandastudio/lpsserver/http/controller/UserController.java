package ru.quandastudio.lpsserver.http.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.Result;
import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.PictureManager;
import ru.quandastudio.lpsserver.data.entities.Picture;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.http.model.MessageWrapper;
import ru.quandastudio.lpsserver.http.model.SignUpRequest;
import ru.quandastudio.lpsserver.http.model.SignUpResponse;
import ru.quandastudio.lpsserver.models.FriendModeResult;
import ru.quandastudio.lpsserver.models.RequestType;
import ru.quandastudio.lpsserver.models.LPSMessage.LPSFriendModeRequest;
import ru.quandastudio.lpsserver.util.ValidationUtil;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

	@Autowired
	private ServerContext context;

	@RequestMapping(value = "/{id}/picture", produces = { MediaType.TEXT_PLAIN_VALUE, "image/*" })
	public ResponseEntity<ByteArrayResource> getPicture(@PathVariable(value = "id") String userId) {
		return Result.of(() -> new User(Integer.parseInt(userId)))
				.map((User user) -> context.getPictureManager().getPictureByUserId(user))
				.flatMap((Optional<Picture> picture) -> Result.from(picture, "No picture"))
				.map((Picture p) -> {
					boolean isBase64 = p.getType() == Picture.Type.BASE64;
					ByteArrayResource res = new ByteArrayResource(p.getImageData());
					return ResponseEntity.ok()
							.contentType(MediaType.parseMediaType(
									isBase64 ? MediaType.APPLICATION_OCTET_STREAM_VALUE : p.getType().getMediaType()))
							.contentLength(res.contentLength())
							.header("Base64-Encoded", String.valueOf(isBase64))
							.body(res);
				})
				.onError((Exception e) -> {
					e.printStackTrace();
				})
				.getOr(ResponseEntity.notFound().build());
	}

	private static final List<String> SUPPORTED_TYPES = Arrays.asList("base64", "png", "jpeg", "gif");

	@PostMapping("/picture")
	public ResponseEntity<MessageWrapper<String>> updatePicture(@AuthenticationPrincipal User user,
			@RequestParam("t") String type, @RequestParam("hash") String hash, @RequestBody byte[] body) {
		return Result.success("ok")
				.check(() -> hash.length() == 32, "Invalid hash code")
				.check(() -> SUPPORTED_TYPES.contains(type), "Unsupported image type")
				.check(() -> body.length < 64 * 1024, "Image too big")
				.apply((String imageData) -> {
					final Picture.Type t = Picture.Type.valueOf(type.toUpperCase());
					PictureManager pics = context.getPictureManager();
					if (!Objects.equals(hash, user.getAvatarHash())) {
						Picture pic = pics.getPictureByUserId(user).orElse(new Picture(user, body, t));
						pic.setImageData(body);
						pic.setType(t);
						pics.save(pic);
						context.getUserManager().updateHash(user, hash);
					}
				})
				.wrap()
				.toResponse();
	}

	@DeleteMapping("/picture")
	@ResponseStatus(HttpStatus.OK)
	public void deletePicture(@AuthenticationPrincipal User user) {
		context.getPictureManager().deletePictureByUser(user);
	}

	@PostMapping("/")
	public ResponseEntity<MessageWrapper<SignUpResponse>> signUp(@RequestBody SignUpRequest request) {
		final Optional<String> firstError = ValidationUtil.validateMessage(request);

		return Result.success(request)
				.check(() -> firstError.isEmpty(), firstError.orElse(null))
				.flatMap(context.getUserManager()::logIn)
				.map((User u) -> new SignUpResponse(u))
				.wrap()
				.toOkResponse();
	}

	@PostMapping("/token/{token}")
	@ResponseStatus(HttpStatus.OK)
	public void updateToken(@AuthenticationPrincipal User user, @PathVariable("token") String token) {
		context.getUserManager().updateToken(user, token);
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
			if (context.getFriendshipManager().isFriends(current, oppUser)) {
				player.sendMessage(new LPSFriendModeRequest(current, FriendModeResult.DENIED));
			} else
				player.sendMessage(new LPSFriendModeRequest(current, FriendModeResult.NOT_FRIEND));
		});
	}

}
