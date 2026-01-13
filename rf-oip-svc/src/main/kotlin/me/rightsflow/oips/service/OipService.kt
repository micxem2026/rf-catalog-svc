package me.rightsflow.oips.service

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import me.rightsflow.clients.feign.ContractConstraintClient
import me.rightsflow.common.config.SecuritySubjectProvider
import me.rightsflow.common.exception.ConstraintException
import me.rightsflow.common.exception.EntityNotFoundWithClsException
import me.rightsflow.common.util.DurationUtil
import me.rightsflow.oips.dto.request.OipCreateRequest
import me.rightsflow.oips.dto.request.OipUpdateRequest
import me.rightsflow.oips.dto.response.OipDto
import me.rightsflow.oips.dto.response.ParentInfo
import me.rightsflow.oips.entity.Oip
import me.rightsflow.oips.entity.OipHierarchy
import me.rightsflow.oips.repository.OipHierarchyRepository
import me.rightsflow.oips.repository.OipRepository
import me.rightsflow.pge.dto.PropertyDataDto
import me.rightsflow.pge.dto.PropertyUpdateBatchDto
import me.rightsflow.pge.dto.PropertyUpdateBatchRequest
import me.rightsflow.pge.service.PgeService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class OipService(
    private val repo: OipRepository,
    private val ohRepo: OipHierarchyRepository,
    private val subProvider: SecuritySubjectProvider,
    private val contractConstraintClient: ContractConstraintClient,
    private val pgeService: PgeService,
    @PersistenceContext private val em: EntityManager
) {

    private val OIP_CODE_PG = "PG_OIP_VIDEO_PROP"

    fun getById(id: Int): OipDto {
        val oip = repo.findById(id).orElseThrow { EntityNotFoundWithClsException(id, Oip::class.java) }
        val parents = getParentInfos(id)
        val props = pgeService.getPgData(OIP_CODE_PG, listOf(id.toLong()), subProvider.currentSub())
        return oip.toDto(parents, props)
    }

    fun findByFilter1(
        idOipSuperType: Int?,
        idOipType: Int?,
        nodeType: Oip.NodeType?,
        filter: String?,
        pageable: Pageable
    ): Page<OipDto> =
        when (nodeType) {
            null -> repo.findByFilter(
                idOipSuperType, idOipType, null, null,
                filter, pageable
            ).map { it.toDto() }

            Oip.NodeType.ROOT -> repo.findByFilter(
                idOipSuperType, idOipType, true, false,
                filter, pageable
            ).map { it.toDto() }

            Oip.NodeType.LEAF -> repo.findByFilter(
                idOipSuperType, idOipType, false, true,
                filter, pageable
            ).map { it.toDto() }

            Oip.NodeType.BRANCH -> repo.findByFilter(
                idOipSuperType, idOipType, true, true,
                filter, pageable
            ).map { it.toDto() }

            Oip.NodeType.ISOLATED -> repo.findByFilter(
                idOipSuperType, idOipType, false, false,
                filter, pageable
            ).map { it.toDto() }
        }

    fun findByFilter(
        idOipSuperType: Int?,
        idOipType: Int?,
        nodeType: Oip.NodeType?,
        filter: String?,
        pageable: Pageable
    ): Page<OipDto> {
        val (hasChildren, hasParent) = when (nodeType) {
            null -> Pair(null, null)
            Oip.NodeType.ROOT -> Pair(null, false)
            Oip.NodeType.LEAF -> Pair(false, true)
            Oip.NodeType.BRANCH -> Pair(true, true)
            Oip.NodeType.ISOLATED -> Pair(false, false)
        }

        val page = repo.findByFilter(
            idOipSuperType,
            idOipType,
            hasChildren,
            hasParent,
            filter,
            pageable
        )

        // Собрать все ID из страницы
        val oipIds = page.content.mapNotNull { it.id }

        // Получить родителей одним запросом для всех OIP на странице
        val parentsMap = getParentsMapForOips(oipIds)

        // Получить все свойства одним запросом для всех OIP на странице
        val propsMap = pgeService.getPgData(OIP_CODE_PG, oipIds.map { it.toLong() }, subProvider.currentSub())
            .groupBy { it.idEntity }

        return page.map { oip ->
            val parents = parentsMap[oip.id] ?: emptyList()
            oip.toDto(parents, propsMap.getOrDefault(oip.id!!.toLong(), emptyList()))
        }
    }

    @Transactional
    fun create(req: OipCreateRequest): OipDto {
        val e = Oip(
            guid = req.guid,
            idOipSuperType = requireNotNull(req.idOipSuperType) { "idOipSuperType is required" },
            idOipType = requireNotNull(req.idOipType) { "idOipType is required" },
            name = requireNotNull(req.name) { "name is required" },
            partNum = req.partNum,
            partCount = req.partCount,
            duration = DurationUtil.fromStringHHmmss(req.duration),
            description = req.description,
            nativeName = req.nativeName,
            releaseYear = req.releaseYear
        ).apply {
            createdBy = subProvider.currentSub()
            createdAt = OffsetDateTime.now()
        }

        repo.saveAndFlush(e)
        em.refresh(e)

        if (!req.propsUpdate.isEmpty())
            pgeService.updatePropertiesBatch(PropertyUpdateBatchRequest(req.propsUpdate.map {
                PropertyUpdateBatchDto(e.id!!.toLong(), OIP_CODE_PG, it.property, it.value)
            }))

        return getById(e.id!!)
    }

    @Transactional
    fun update(id: Int, req: OipUpdateRequest): OipDto {
        val e = repo.findById(id).orElseThrow { EntityNotFoundWithClsException(id, Oip::class.java) }
        // NOT NULL поля: если пришло null — НЕ меняем
        req.idOipSuperType?.let { e.idOipSuperType = it }
        req.idOipType?.let { e.idOipType = it }
        req.name?.let { e.name = it }
        req.partNum?.let { e.partNum = it }
        req.partCount?.let { e.partCount = it }
        // GUID nullable — меняем если прислали
        if (req.guid != null) e.guid = req.guid
        // DURATION nullable — записываем что пришло (включая null)
        e.duration = DurationUtil.fromStringHHmmss(req.duration)
        // DESCRIPTION nullable — записываем что пришло (включая null)
        e.description = req.description
        e.nativeName = req.nativeName
        e.releaseYear = req.releaseYear
        e.updatedBy = subProvider.currentSub()
        e.updatedAt = OffsetDateTime.now()

        repo.saveAndFlush(e)
        em.refresh(e)

        if (!req.propsUpdate.isEmpty())
            pgeService.updatePropertiesBatch(PropertyUpdateBatchRequest(req.propsUpdate.map {
                PropertyUpdateBatchDto(e.id!!.toLong(), OIP_CODE_PG, it.property, it.value)
            }))

        return getById(e.id!!)
    }

    @Transactional
    fun delete(id: Int) {
        if (!repo.existsById(id)) throw EntityNotFoundWithClsException(id, Oip::class.java)
        if (!contractConstraintClient.checkOipConstraint(id)) {
            repo.deleteById(id)
        } else {
            throw ConstraintException(id, Oip::class.java)
        }
    }

    // Получить список родителей для одного OIP
    private fun getParentInfos(oipId: Int): List<ParentInfo> {
        @Suppress("UNCHECKED_CAST")
        val result = em.createQuery(
            """
            select p.id, p.name
            from OipHierarchy h
            join Oip p on p.id = h.idParent
            where h.idOip = :idOip
            order by h.idParent
            """
        )
            .setParameter("idOip", oipId)
            .resultList as List<Array<Any>>

        return result.map { ParentInfo(id = it[0] as Int, name = it[1] as String) }
    }

    // Получить конкретного родителя для одного OIP
    private fun getParentInfo(oipId: Int): List<ParentInfo> {
        @Suppress("UNCHECKED_CAST")
        val result = em.createQuery(
            """
            select p.id, p.name
            from Oip p
            where p.id = :idOip
            """
        )
            .setParameter("idOip", oipId)
            .resultList as List<Array<Any>>

        return result.map { ParentInfo(id = it[0] as Int, name = it[1] as String) }
    }

    // Получить Map<OipId, List<ParentInfo>> для множества OIP одним запросом
    private fun getParentsMapForOips(oipIds: List<Int>): Map<Int, List<ParentInfo>> {
        if (oipIds.isEmpty()) return emptyMap()

        @Suppress("UNCHECKED_CAST")
        val results = em.createQuery(
            """
            select h.idOip, p.id, p.name
            from OipHierarchy h
            join Oip p on p.id = h.idParent
            where h.idOip in :oipIds
            order by h.idOip, h.idParent
            """
        )
            .setParameter("oipIds", oipIds)
            .resultList as List<Array<Any>>

        return results.groupBy(
            keySelector = { it[0] as Int },
            valueTransform = { ParentInfo(id = it[1] as Int, name = it[2] as String) }
        )
    }

    /**
     * Получить все ОИС в иерархии заданного ОИС
     * @param oipId ID искомого ОИС
     * @param direction направление обхода (UP/DOWN/BOTH)
     * @return список всех ОИС из иерархии
     */
    fun findAllInHierarchy(
        oipId: Int,
        direction: Oip.HierarchyDirection = Oip.HierarchyDirection.BOTH
    ): List<OipDto> {
        // Проверяем что OIP существует
        if (!repo.existsById(oipId)) {
            throw EntityNotFoundWithClsException(oipId, Oip::class.java)
        }

        // Получаем все ID в иерархии в зависимости от направления
        val hierarchyIds = when (direction) {
            Oip.HierarchyDirection.UP -> findAncestorIds(oipId)
            Oip.HierarchyDirection.DOWN -> findDescendantIds(oipId)
            Oip.HierarchyDirection.BOTH -> findAllHierarchyIds(oipId)
        }

        if (hierarchyIds.isEmpty()) {
            return emptyList()
        }

        // Загружаем все OIP по найденным ID
        val oips = em.createQuery(
            """
            SELECT o FROM Oip o
            LEFT JOIN FETCH o.oipSuperType
            LEFT JOIN FETCH o.oipType
            WHERE o.id IN :ids
            ORDER BY o.id
            """,
            Oip::class.java
        )
            .setParameter("ids", hierarchyIds)
            .resultList

        // Получаем родителей для всех OIP одним запросом
        val parentsMap = getParentsMapForOips(oips.mapNotNull { it.id })

        return oips.map { oip ->
            val parents = parentsMap[oip.id] ?: emptyList()
            oip.toDto(parents)
        }
    }

    /**
     * Найти все ID предков (родителей) рекурсивно вверх по иерархии
     */
    private fun findAncestorIds(oipId: Int): List<Int> {
        @Suppress("UNCHECKED_CAST")
        val result = em.createNativeQuery(
            """
            WITH RECURSIVE ancestors AS (
                -- Исходный узел
                SELECT id FROM klf_oip WHERE id = :oipId
                
                UNION
                
                -- Рекурсивно поднимаемся вверх по иерархии
                SELECT h.id_parent
                FROM klf_oip_hierarchy h
                INNER JOIN ancestors a ON a.id = h.id_oip
            )
            SELECT DISTINCT id FROM ancestors ORDER BY id
            """
        )
            .setParameter("oipId", oipId)
            .resultList as List<Int>

        return result
    }

    /**
     * Найти все ID потомков (детей) рекурсивно вниз по иерархии
     */
    private fun findDescendantIds(oipId: Int): List<Int> {
        @Suppress("UNCHECKED_CAST")
        val result = em.createNativeQuery(
            """
            WITH RECURSIVE descendants AS (
                -- Исходный узел
                SELECT id FROM klf_oip WHERE id = :oipId
                
                UNION
                
                -- Рекурсивно спускаемся вниз по иерархии
                SELECT h.id_oip
                FROM klf_oip_hierarchy h
                INNER JOIN descendants d ON d.id = h.id_parent
            )
            SELECT DISTINCT id FROM descendants ORDER BY id
            """
        )
            .setParameter("oipId", oipId)
            .resultList as List<Int>

        return result
    }

    /**
     * Найти все ID в полной иерархии (и предки, и потомки)
     */
    private fun findAllHierarchyIds(oipId: Int): List<Int> {
        @Suppress("UNCHECKED_CAST")
        val result = em.createNativeQuery(
            """
            WITH RECURSIVE ancestors AS (
                -- Исходный узел
                SELECT id FROM klf_oip WHERE id = :oipId
                
                UNION
                
                -- Рекурсивно поднимаемся вверх
                SELECT h.id_parent
                FROM klf_oip_hierarchy h
                INNER JOIN ancestors a ON a.id = h.id_oip
            ),
            descendants AS (
                -- Исходный узел
                SELECT id FROM klf_oip WHERE id = :oipId
                
                UNION
                
                -- Рекурсивно спускаемся вниз
                SELECT h.id_oip
                FROM klf_oip_hierarchy h
                INNER JOIN descendants d ON d.id = h.id_parent
            )
            SELECT DISTINCT id FROM ancestors
            UNION
            SELECT DISTINCT id FROM descendants
            ORDER BY id
            """
        )
            .setParameter("oipId", oipId)
            .resultList as List<Int>

        return result
    }

    fun findChildrenByParent(parentId: Int, pageable: Pageable): Page<OipDto> {
        val parents = getParentInfo(parentId)
        return ohRepo.findChildrenSortedByPartNum(parentId, pageable).map { it.toOipDto(parents) }
    }

    private fun Oip.toDto(parents: List<ParentInfo> = emptyList(), properties: List<PropertyDataDto> = emptyList()) =
        OipDto(
            id = this.id!!,
            guid = this.guid,
            idOipSuperType = this.idOipSuperType,
            idOipType = this.idOipType,
            name = this.name,
            partNum = this.partNum,
            partCount = this.partCount,
            duration = DurationUtil.toStringHHmmss(this.duration),
            oipSuperTypeName = this.oipSuperType?.name ?: "",
            oipTypeName = this.oipType?.name ?: "",
            description = this.description,
            hasParent = this.hasParent,
            hasChildren = this.hasChildren,
            childrenCount = this.childrenCount,
            nativeName = this.nativeName,
            fullName = this.fullName,
            releaseYear = this.releaseYear,
            parents = parents,
            properties = properties,
            createdBy = this.createdBy,
            createdAt = this.createdAt,
            updatedBy = this.updatedBy,
            updatedAt = this.updatedAt
        )

    private fun OipHierarchy.toOipDto(
        parents: List<ParentInfo> = emptyList(),
        properties: List<PropertyDataDto> = emptyList()
    ) = OipDto(
        id = this.idOip,
        guid = this.oip?.guid,
        idOipSuperType = this.oip!!.idOipSuperType,
        idOipType = this.oip!!.idOipType,
        name = oip?.name ?: "",
        partNum = this.oip!!.partNum,
        partCount = this.oip!!.partCount,
        duration = DurationUtil.toStringHHmmss(this.oip?.duration),
        oipSuperTypeName = this.oip?.oipSuperType?.name ?: "",
        oipTypeName = this.oip?.oipType?.name ?: "",
        description = this.oip?.description ?: "",
        hasParent = this.oip!!.hasParent,
        hasChildren = this.oip!!.hasChildren,
        childrenCount = this.oip!!.childrenCount,
        nativeName = this.oip?.nativeName,
        fullName = this.oip?.fullName,
        releaseYear = this.oip?.releaseYear,
        parents = parents,
        properties = properties,
        createdBy = this.oip!!.createdBy,
        createdAt = this.oip!!.createdAt,
        updatedBy = this.oip?.updatedBy,
        updatedAt = this.oip?.updatedAt
    )
}