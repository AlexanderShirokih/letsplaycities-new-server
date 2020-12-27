package ru.quandastudio.lpsserver.data.entities;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Arrays;

/**
 * Used to store user avatar to database
 *
 * @author Alexander Shirokikh
 */
@Entity
@Table
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Picture implements Serializable {

    private static final long serialVersionUID = 2L;

    public enum Type {
        BASE64(null), PNG("image/png"), JPEG("image/jpeg"), GIF("image/gif");

        private final String name;

        Type(String name) {
            this.name = name;
        }

        public String getMediaType() {
            return name;
        }

        public static Type fromMimeType(String type) {
            return Arrays.stream(values()).filter(baseType -> type.equals(baseType.name))
                    .findFirst().orElse(null);
        }
    }

    @Id
    @Column(name = "owner_id", nullable = false, insertable = false, updatable = false)
    Integer ownerId;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image", nullable = false)
    private byte[] imageData;

    // b64 or raw
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private Type type;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "owner_id", referencedColumnName = "id")
    private User owner;

    @Override
    public int hashCode() {
        return 42;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Picture picture = (Picture) o;
        return ownerId.equals(picture.ownerId) &&
                type == picture.type &&
                owner.equals(picture.owner);
    }

    public Picture(User owner, byte[] imageData, Type type) {
        this.ownerId = owner.getId();
        this.owner = owner;
        this.imageData = imageData;
        this.type = type;
    }
}
