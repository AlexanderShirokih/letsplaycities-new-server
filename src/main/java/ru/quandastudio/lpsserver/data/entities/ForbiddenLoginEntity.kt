package ru.quandastudio.lpsserver.data.entities

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Table(name = "ForbiddenLogin")
@Entity
class ForbiddenLoginEntity(
    @Id
    @Column(name = "value", nullable = false)
    var value: String
)