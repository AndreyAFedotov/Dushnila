package com.iceekb.dushnila.jpa.repo;

import com.iceekb.dushnila.jpa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepo extends JpaRepository<User, Long> {

    @Query("""
            SELECT u FROM User u
            WHERE u.tgId = :userTgId
            """)
    User findByTgId(Long userTgId);

}
