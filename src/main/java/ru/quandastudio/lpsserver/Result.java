package ru.quandastudio.lpsserver;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Result<T> {
	private final T data;
	private final Exception error;

	public static Result<Object> empty() {
		return new Result<Object>(new Object(), null);
	}

	public static <T> Result<T> success(@NonNull T data) {
		return new Result<T>(data, null);
	}

	public static <T> Result<T> error(@NonNull Exception error) {
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

	public Result<T> check(Predicate<T> condition, String errorMsg) {
		if (isSuccess() && !condition.test(data)) {
			return error(errorMsg);
		}
		return this;
	}

	public <R> Result<R> map(Function<T, R> mapper) {
		if (isSuccess()) {
			R newData = mapper.apply(data);
			return success(Objects.requireNonNull(newData));
		}
		return error(error);
	}

	public <R> Result<R> flatMap(Function<T, Result<R>> mapper) {
		if (isSuccess()) {
			return Objects.requireNonNull(mapper.apply(data));
		}
		return error(error);
	}

	public T getOr(T defaultValue) {
		if (isSuccess()) {
			return data;
		}
		return defaultValue;
	}

	public Result<T> or(@NonNull Result<T> defaultResult) {
		if (isSuccess()) {
			return this;
		}
		return defaultResult;
	}

	public boolean hasErrors() {
		return error != null;
	}

	public boolean isSuccess() {
		return data != null;
	}

}
