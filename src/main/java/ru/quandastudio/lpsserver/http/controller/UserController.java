package ru.quandastudio.lpsserver.http.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.Result;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.entities.Picture;
import ru.quandastudio.lpsserver.data.entities.User;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

	@Autowired
	private ServerContext context;

	@RequestMapping(value = "/{id}/picture", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<ByteArrayResource> getPicture(@PathVariable(value = "id") String userId) {
		return Result.of(() -> new User(Integer.parseInt(userId)))
				.map((User user) -> context.getPictureManager().getPictureByUserId(user))
				.flatMap((Optional<Picture> picture) -> Result.from(picture, "No picture"))
				.map((Picture p) -> new ByteArrayResource(p.getImageData()))
				.map((ByteArrayResource resource) -> ResponseEntity.ok()
						.contentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
						.contentLength(resource.contentLength())
						.header("Base64-Encoded", "true")
						.body(resource))
				.getOr(ResponseEntity.notFound().build());
	}

}
