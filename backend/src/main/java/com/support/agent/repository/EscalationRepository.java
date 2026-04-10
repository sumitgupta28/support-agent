package com.support.agent.repository;

import com.support.agent.model.Escalation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EscalationRepository extends JpaRepository<Escalation, Long> {

    List<Escalation> findBySessionId(String sessionId);

    List<Escalation> findByResolvedFalseOrderByCreatedAtDesc();
}
