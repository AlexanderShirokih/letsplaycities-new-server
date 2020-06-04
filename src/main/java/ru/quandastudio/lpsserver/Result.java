package ru.quandastudio.lpsserver;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import ru.quandastudio.lpsserver.http.model.MessageWrapper;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Result<T> {

	@Getter(onMethod_ = { @Deprecated })
	private final T data;
	@Getter
	private final Exception error;

	public static <T> Result<T> success(@NonNull T data) {
		return new Result<>(data, null);
	}

	public static <T> Result<T> error(@NonNull Exception error) {
		return new Result<>(null, error);
	}

	public static <T> Result<T> error(String error) {
		return error(new RuntimeException(error));
	}

	public static Result<Object> empty() {
		return success(new Object());
	}

	public static <T> Result<T> of(Supplier<T> supplier) {
		try {
			return success(supplier.get());
		} catch (Exception e) {
			return error(e);
		}
	}

	public static <T> Result<T> from(@NonNull Optional<T> optional, String errMsg) {
		if (optional.isPresent())
			try {
				return success(optional.get());
			} catch (NoSuchElementException e) {
				return error(e);
			}
		else
			return error(errMsg);
	}

	public Result<T> check(Predicate<T> condition, String errorMsg) {
		if (isSuccess() && !condition.test(data)) {
			return error(errorMsg);
		}
		return this;
	}

	public Result<T> check(BooleanSupplier condition, String errorMsg) {
		if (isSuccess() && !condition.getAsBoolean()) {
			return error(errorMsg);
		}
		return this;
	}

	public Result<T> apply(Consumer<T> cons) {
		if (isSuccess())
			cons.accept(data);
		return this;
	}

	public Result<T> onError(Consumer<Exception> cons) {
		if (hasErrors())
			cons.accept(error);
		return this;
	}

	public <R> Result<R> map(Function<T, R> mapper) {
		if (isSuccess()) {
			try {
				R newData = mapper.apply(data);
				return success(Objects.requireNonNull(newData));
			} catch (Exception e) {
				return error(e);
			}
		}
		return error(error);
	}

	public Result<T> mapError(Function<Exception, Exception> errorMapper) {
		if (isSuccess())
			return this;
		return error(errorMapper.apply(error));
	}

	public <R> Result<R> flatMap(Function<T, Result<R>> mapper) {
		if (isSuccess()) {
			try {
				return Objects.requireNonNull(mapper.apply(data));
			} catch (Exception e) {
				return error(e);
			}
		}
		return error(error);
	}

	public T get() throws RuntimeException {
		if (isSuccess())
			return data;

		if (error instanceof RuntimeException)
			throw (RuntimeException) error;
		else
			throw new RuntimeException(error);
	}

	public T getOr(T defaultValue) {
		if (isSuccess()) {
			return data;
		}
		return defaultValue;
	}

	public T getOr(Function<Exception, T> func) {
		if (isSuccess()) {
			return data;
		}

		return func.apply(error);
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

	public MessageWrapper<T> wrap() {
		return new MessageWrapper<>(data, error == null ? null : error.getMessage());
	}

}
