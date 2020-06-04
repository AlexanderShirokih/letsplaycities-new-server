package ru.quandastudio.lpsserver.data.entities;

import java.io.Serializable;
import java.util.Objects;

public class BanlistIdKey implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer baner;

    private Integer banned;

    @Override
    public int hashCode() {
        return Objects.hash(baner, banned);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BanlistIdKey other = (BanlistIdKey) obj;
        return Objects.equals(baner, other.baner) && Objects.equals(banned, other.banned);
    }
}
