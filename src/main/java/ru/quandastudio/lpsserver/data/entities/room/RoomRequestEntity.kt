package ru.quandastudio.lpsserver.data.entities.room

import org.hibernate.annotations.CreationTimestamp
import ru.quandastudio.lpsserver.data.entities.User
import ru.quandastudio.lpsserver.data.entities.cities.CountryGroupEntity
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "RoomRequest")
class RoomRequestEntity(
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @Column(name = "hash", nullable = true)
    var hash: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    var requester: User? = null,

    @ManyToOne
    @JoinColumn(name = "country_group_id", nullable = true)
    var countryGroup: CountryGroupEntity? = null,

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false)
    var creationDate: Timestamp? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: RoomRequestStatusEntity = RoomRequestStatusEntity.NEW
)