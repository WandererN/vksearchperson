package com.jh.vkstattool

import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.client.actors.UserActor
import com.vk.api.sdk.objects.groups.Group
import com.vk.api.sdk.objects.groups.GroupFull
import com.vk.api.sdk.objects.users.UserFull
import com.vk.api.sdk.queries.users.UserField
import org.slf4j.LoggerFactory

class VkPersonFinder(private val vk: VkApiClient, private val actor: UserActor) {
    private val log = LoggerFactory.getLogger(VkPersonFinder::class.java)

    private fun getFullGroupMembersIds(groupId: Int): Set<Int> {
        val res = HashSet<Int>()
        var currentOffset = 0
        while (true) {
            val groupResp =
                vk.groups().getMembers(actor).groupId(groupId.toString()).offset(currentOffset)
                    .execute()
            res.addAll(groupResp.items)
            currentOffset += groupResp.items.size
            log.info("Fetching members of $groupId: $currentOffset of ${groupResp.count}")
            if (currentOffset >= groupResp.count)
                break
        }
        return res;
    }

    private fun getMutualFriends(friends: List<Int>): Set<Int> {
        val resSet = HashSet<Int>()
        if (friends.size == 1) {
            resSet.addAll(vk.friends().get(actor).userId(friends[0]).execute().items)
            return resSet
        }

        friends.forEachIndexed { i, userId ->
            if (i == 1) {
                resSet.addAll(
                    vk.friends().getMutual(actor).sourceUid(friends[0]).targetUid(userId)
                        .execute()
                )
            } else if (i > 1) {
                resSet.retainAll(
                    vk.friends().getMutual(actor).sourceUid(friends[0]).targetUid(userId)
                        .execute()
                )
            }
        }
        return resSet
    }

    fun findUserIds(commonFriends: List<Int>, commonGroups: List<Int>): Set<Int> {
        val subtractionSet = HashSet<Int>()
        if (commonGroups.isNotEmpty()) {
            commonGroups.forEachIndexed { i, group ->
                val groupUsers = getFullGroupMembersIds(group)
                if (i == 0) {
                    subtractionSet.addAll(groupUsers)
                } else {
                    subtractionSet.retainAll(groupUsers)
                }
            }
            if (commonFriends.isNotEmpty())
                subtractionSet.retainAll(getMutualFriends(commonFriends))
        } else
            subtractionSet.addAll(getMutualFriends(commonFriends))
        return subtractionSet
    }

    fun getUsersByIds(userIds: Set<Int>): List<UserFull> =
        vk.users().get(actor).userIds(userIds.map { it.toString() }.toMutableList())
            .fields(UserField.NICKNAME)
            .execute()

    fun getGroupsByIds(groupsId: Set<Int>): List<GroupFull> = if (groupsId.isNotEmpty())
        vk.groups().getById(actor).groupIds(groupsId.map { it.toString() }.toMutableList())
            .execute() else emptyList()

    fun findUsersWithFullInfo(commonFriends: List<Int>, commonGroups: List<Int>): List<UserFull> =
        getUsersByIds(findUserIds(commonFriends, commonGroups))

    fun getUserGroups(): List<Group> {
        return vk.groups().getExtended(actor).execute().items
    }
}