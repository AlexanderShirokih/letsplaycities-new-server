package ru.quandastudio.lpsserver.data.entities;

import java.io.Serializable;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CompositeFriendshipKey implements Serializable {
	private static final long serialVersionUID = 1L;

	private Integer sender;

	private Integer receiver;

	@Override
	public int hashCode() {
		return Objects.hash(receiver, sender);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CompositeFriendshipKey other = (CompositeFriendshipKey) obj;
		return Objects.equals(receiver, other.receiver) && Objects.equals(sender, other.sender);
	}

}
