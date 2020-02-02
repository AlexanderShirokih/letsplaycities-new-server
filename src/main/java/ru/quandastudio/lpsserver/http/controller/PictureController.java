package ru.quandastudio.lpsserver.http.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.entities.Picture;
import ru.quandastudio.lpsserver.data.entities.User;

@RestController
@RequiredArgsConstructor
public class PictureController {

	@Autowired
	private ServerContext context;

	@RequestMapping(value = "/user/{id}/picture", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<ByteArrayResource> getPicture(@PathVariable(value = "id") String userId) {
		User user;
		try {
			user = new User(Integer.parseInt(userId));
		} catch (NumberFormatException e) {
			return ResponseEntity.notFound().build();
		}

		return context.getPictureManager()
				.getPictureByUserId(user)
				.map((Picture p) -> new ByteArrayResource(p.getImageData()))
				.map((ByteArrayResource resource) -> ResponseEntity.ok()
						.contentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
						.contentLength(resource.contentLength())
						.header("Base64-Encoded", "true")
						.body(resource))
				.orElse(ResponseEntity.notFound().build());
	}
}
