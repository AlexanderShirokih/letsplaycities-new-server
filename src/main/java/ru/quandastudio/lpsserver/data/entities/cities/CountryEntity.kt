package ru.quandastudio.lpsserver.data.entities.cities

import javax.persistence.*

@Entity
@Table(name = "Country")
class CountryEntity(
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @Column(name = "name", nullable = false)
    var name: String? = null,

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "countries")
    var groups: Set<CountryGroupEntity> = emptySet()
)
