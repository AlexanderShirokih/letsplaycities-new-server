package ru.quandastudio.lpsserver;

import java.util.Optional;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Result<T> {
	private final T data;
	private final Exception error;

	public static <T> Result<T> success(T data) {
		return new Result<T>(data, null);
	}

	public static <T> Result<T> error(Exception error) {
		return new Result<T>(null, error);
	}

	public static <T> Result<T> error(String error) {
		return error(new IllegalStateException(error));
	}

	public static <T> Result<T> from(@NonNull Optional<T> optional, String errMsg) {
		if (optional.isPresent())
			return success(optional.get());
		else
			return error(errMsg);
	}

	public boolean hasErrors() {
		return error != null;
	}

}
