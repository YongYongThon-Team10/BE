package com.yongyongthon.be.event.data.repository;

import com.yongyongthon.be.event.data.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUserEmailAndEventId(String email, String eventId);

    @Query("select f.event.id from Favorite f where f.user.email = :email")
    Set<String> findEventIdsByUserEmail(@Param("email") String email);

    long deleteByUserEmailAndEventId(String email, String eventId);
}
