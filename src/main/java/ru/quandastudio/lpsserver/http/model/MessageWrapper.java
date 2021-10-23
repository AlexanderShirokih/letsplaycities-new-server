package ru.quandastudio.lpsserver.http.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;

@RequiredArgsConstructor
@Getter
public class MessageWrapper<T> {

    private final T data;

    private final String error;

    public static <T> MessageWrapper<T> of(@NonNull T data) {
        return new MessageWrapper<T>(data, null);
    }

    @NotNull
    public ResponseEntity<MessageWrapper<T>> toResponse() {
        if (error == null)
            return ResponseEntity.ok(this);
        return ResponseEntity.badRequest().body(this);
    }

    public ResponseEntity<MessageWrapper<T>> toOkResponse() {
        return ResponseEntity.ok(this);
    }
}
