package com.cupflow.pos.system.infrastructure;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DatabaseProbeMapper {

    @Select("SELECT 1")
    int probe();
}
