package ru.quandastudio.lpsserver.data.entities.cities

import javax.persistence.*

@Entity
@Table(name = "CountryGroup")
class CountryGroupEntity(
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @Column(name = "name", nullable = false)
    var name: String? = null,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "CountryGroupAssignments",
        joinColumns = [JoinColumn(name = "group_id")],
        inverseJoinColumns = [JoinColumn(name = "country_id")]
    )
    val countries: Set<CountryEntity>,
)