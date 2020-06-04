package ru.quandastudio.lpsserver.http.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.http.model.UpdateMessage;
import ru.quandastudio.lpsserver.http.model.UpdateMessage.Dictionary;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UpdateController {

	private final ServerContext context;

	@RequestMapping("/update")
	public UpdateMessage updateMessage() {
		final int lastDbVersion = context.getDictionary().getLastDatabaseVersion();
		return new UpdateMessage(new Dictionary(lastDbVersion));
	}

	@RequestMapping(value = "/data-{version}.bin", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<ByteArrayResource> getDictionary(@PathVariable(value = "version") String version) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");

		return context.getDictionary().getDatabaseFile(version).flatMap((File path) -> {
			try {
				return Optional.of(new ByteArrayResource(Files.readAllBytes(path.toPath())));
			} catch (IOException e) {
				log.error("Error sending database file. ", e);
			}
			return Optional.empty();
		}).map((ByteArrayResource resource) -> ResponseEntity.ok()
				.headers(headers)
				.contentType(MediaType.parseMediaType("application/octet-stream"))
				.contentLength(resource.contentLength())
				.body(resource)).orElse(ResponseEntity.notFound().build());
	}
}
