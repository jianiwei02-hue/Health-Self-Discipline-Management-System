package com.health.health_system.repository;

import com.health.health_system.entity.ItemLibrary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ItemLibraryRepository extends JpaRepository<ItemLibrary, Integer> {

    // 按类型查询（FOOD 或 SPORT）
    List<ItemLibrary> findByType(String type);

    // 按名称模糊查询
    List<ItemLibrary> findByNameContaining(String name);

    // 按类型和名称精确查询
    Optional<ItemLibrary> findByTypeAndName(String type, String name);

    // 按类型和名称模糊查询
    List<ItemLibrary> findByTypeAndNameContaining(String type, String name);

    // 查询所有有 MET 值的运动
    @Query("SELECT i FROM ItemLibrary i WHERE i.type = 'SPORT' AND i.metValue IS NOT NULL")
    List<ItemLibrary> findAllSportsWithMet();

    // 根据运动名称获取 MET 值
    @Query("SELECT i.metValue FROM ItemLibrary i WHERE i.type = 'SPORT' AND i.name = :name")
    Double findMetValueBySportName(@Param("name") String name);
}