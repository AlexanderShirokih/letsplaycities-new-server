package ru.quandastudio.lpsserver.data.entities

import javax.persistence.*

@Table(name = "CityEditRequest")
@Entity
data class CityEditRequestEntity(
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    var owner: User,

    @Column(name = "country_code", nullable = false)
    var countryCode: Int,

    @Column(name = "old_name", nullable = true)
    var oldName: String? = null,

    @Column(name = "new_name", nullable = true)
    var newName: String? = null,

    @Column(name = "reason", nullable = true)
    var reason: String? = null,

    @Column(name = "verdict", nullable = true)
    var verdict: String? = null,

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: CityEditRequestStatusEntity = CityEditRequestStatusEntity.NEW,
)