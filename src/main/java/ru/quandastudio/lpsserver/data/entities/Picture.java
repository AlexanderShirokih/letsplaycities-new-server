package ru.quandastudio.lpsserver.data.entities;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

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
    }

    @Id
    private Integer id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image", nullable = false)
    private byte[] imageData;

    // b64 or raw
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private Type type;

    @Override
    public int hashCode() {
        return owner.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Picture other = (Picture) obj;
        return Objects.equals(owner, other.owner);
    }

    public Picture(User owner, byte[] imageData, Type type) {
        this.owner = owner;
        this.imageData = imageData;
        this.type = type;
    }
}
