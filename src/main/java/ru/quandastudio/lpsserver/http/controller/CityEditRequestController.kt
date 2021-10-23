package ru.quandastudio.lpsserver.http.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import ru.quandastudio.lpsserver.data.managers.cities.CityEditRequestManager
import ru.quandastudio.lpsserver.data.entities.User
import ru.quandastudio.lpsserver.http.model.MessageWrapper
import ru.quandastudio.lpsserver.models.cities.CityEditRequest
import ru.quandastudio.lpsserver.models.cities.CityEditRequestStatus
import ru.quandastudio.lpsserver.models.cities.CityEditResult

@RestController
@RequestMapping("/cities/edit")
class CityEditRequestController(
    private val cityEditRequestManager: CityEditRequestManager,
) {

    /**
     * Creates new city editing request
     */
    @PostMapping
    fun createRequest(
        @RequestBody request: CityEditRequest,
        @AuthenticationPrincipal user: User,
    ): ResponseEntity<MessageWrapper<String>> {
        return cityEditRequestManager
            .addCityEditRequest(
                owner = user,
                request = request,
            )
            .wrap()
            .toResponse()
    }

    /**
     * Gets a list of requests created by specified user
     */
    @GetMapping
    fun getRequestsList(
        @AuthenticationPrincipal user: User,
    ): List<CityEditResult> = cityEditRequestManager.getRequestList(user)

    /**
     * Gets a list of all opened requests.
     * @param user user who has admin privileges
     */
    @GetMapping("/opened")
    fun getOpenedRequests(
        @AuthenticationPrincipal user: User,
    ): ResponseEntity<MessageWrapper<List<CityEditResult>>> {
        return cityEditRequestManager
            .getOpenedRequests(user)
            .wrap()
            .toResponse()
    }

    /**
     * Deletes previously created editing request
     */
    @DeleteMapping("/{id}")
    fun deleteRequest(@PathVariable("id") requestId: Int, @AuthenticationPrincipal user: User) {
        cityEditRequestManager.deleteRequest(requestId = requestId, owner = user)
    }

    /**
     * Approves new editing request
     */
    @PutMapping("/{id}/approve")
    fun approveRequest(
        @PathVariable("id") requestId: Int,
        @AuthenticationPrincipal user: User,
    ): ResponseEntity<MessageWrapper<String>> {
        return cityEditRequestManager
            .updateStatus(requestId, actor = user, status = CityEditRequestStatus.APPROVED)
            .wrap()
            .toResponse()
    }

    /**
     * Declines new editing request
     */
    @PutMapping("/{id}/decline")
    fun declineRequest(
        @PathVariable("id") requestId: Int,
        @AuthenticationPrincipal user: User,
    ): ResponseEntity<MessageWrapper<String>> {
        return cityEditRequestManager
            .updateStatus(requestId, actor = user, status = CityEditRequestStatus.DECLINED)
            .wrap()
            .toResponse()
    }
}
