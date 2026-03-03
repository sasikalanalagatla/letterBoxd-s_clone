package com.clone.letterboxd.repository;

import com.clone.letterboxd.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByResetToken(String resetToken);
    
    @Query("SELECT COUNT(f) FROM User u JOIN u.followers f WHERE u.id = :userId")
    long countFollowers(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(f) FROM User u JOIN u.following f WHERE u.id = :userId")
    long countFollowing(@Param("userId") Long userId);
    
    @Query("SELECT CASE WHEN (COUNT(f) > 0) THEN true ELSE false END FROM User u JOIN u.following f WHERE u.id = :followerId AND f.id = :followingId")
    boolean isFollowing(@Param("followerId") Long followerId, @Param("followingId") Long followingId);
    
    @Query("SELECT f FROM User u JOIN u.followers f WHERE u.id = :userId")
    List<User> getFollowers(@Param("userId") Long userId);
    
    @Query("SELECT f FROM User u JOIN u.following f WHERE u.id = :userId")
    List<User> getFollowing(@Param("userId") Long userId);
    
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM user_follows WHERE follower_id = :followerId AND following_id = :followingId", nativeQuery = true)
    void removeFollow(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO user_follows (follower_id, following_id) VALUES (:followerId, :followingId)", nativeQuery = true)
    void addFollow(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    List<User> findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
            String username, String displayName);
}
