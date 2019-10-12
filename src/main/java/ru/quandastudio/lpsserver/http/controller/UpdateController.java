package ru.quandastudio.lpsserver.http.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.http.model.UpdateMessage;
import ru.quandastudio.lpsserver.http.model.UpdateMessage.Dictionary;

@RestController
@RequiredArgsConstructor
public class UpdateController {

	@Autowired
	private ServerContext context;

	@RequestMapping("/update")
	public UpdateMessage updateMessage() {
		final int lastDbVersion = context.getDictionary().getLastDatabaseVersion();
		return new UpdateMessage(new Dictionary(lastDbVersion));
	}

	@RequestMapping(value = "/data-{version}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public @ResponseBody byte[] getDictionary(@PathVariable(value = "version") String version) {
		return ("Hello world from " + version).getBytes();
	}
}
