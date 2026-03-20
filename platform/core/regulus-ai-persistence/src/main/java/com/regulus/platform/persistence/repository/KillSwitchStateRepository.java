package com.regulus.platform.persistence.repository;

import com.regulus.platform.persistence.entity.KillSwitchStateEntity;
import com.regulus.platform.persistence.entity.KillSwitchStateEntity.Scope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for kill switch states.
 * Uses optimistic locking via @Version for concurrent updates.
 */
@Repository
public interface KillSwitchStateRepository extends JpaRepository<KillSwitchStateEntity, String> {

    /**
     * Find the global kill switch state.
     */
    @Query("SELECT k FROM KillSwitchStateEntity k WHERE k.scope = 'GLOBAL'")
    Optional<KillSwitchStateEntity> findGlobalState();

    /**
     * Find kill switch state by scope and target.
     */
    Optional<KillSwitchStateEntity> findByScopeAndTargetId(Scope scope, String targetId);

    /**
     * Find all active kill switches.
     */
    List<KillSwitchStateEntity> findByActiveTrue();

    /**
     * Find all kill switches by scope.
     */
    List<KillSwitchStateEntity> findByScope(Scope scope);

    /**
     * Find all active kill switches by scope.
     */
    List<KillSwitchStateEntity> findByScopeAndActiveTrue(Scope scope);

    /**
     * Check if any kill switch is active for a given scope and target.
     * Also checks global kill switch.
     */
    @Query("""
        SELECT CASE WHEN COUNT(k) > 0 THEN true ELSE false END
        FROM KillSwitchStateEntity k
        WHERE k.active = true
          AND (k.scope = 'GLOBAL'
               OR (k.scope = :scope AND k.targetId = :targetId))
        """)
    boolean isBlocked(@Param("scope") Scope scope, @Param("targetId") String targetId);

    /**
     * Check if global kill switch is active.
     */
    @Query("SELECT CASE WHEN COUNT(k) > 0 THEN true ELSE false END FROM KillSwitchStateEntity k WHERE k.scope = 'GLOBAL' AND k.active = true")
    boolean isGloballyBlocked();

    /**
     * Count active kill switches by scope.
     */
    @Query("SELECT k.scope, COUNT(k) FROM KillSwitchStateEntity k WHERE k.active = true GROUP BY k.scope")
    List<Object[]> countActiveByScope();
}
