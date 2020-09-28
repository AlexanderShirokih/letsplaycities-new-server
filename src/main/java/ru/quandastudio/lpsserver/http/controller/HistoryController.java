package ru.quandastudio.lpsserver.http.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.models.HistoryInfo;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/history")
public class HistoryController {

    private final ServerContext context;

    @GetMapping("/")
    @ResponseBody
    public List<HistoryInfo> getHistory(@AuthenticationPrincipal User user) {
        return context.getHistoryManager().getHistoryList(user);
    }
}
