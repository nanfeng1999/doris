/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

suite("fix_leading") {
    // create database and tables
    sql 'DROP DATABASE IF EXISTS fix_leading'
    sql 'CREATE DATABASE IF NOT EXISTS fix_leading'
    sql 'use fix_leading'

    // setting planner to nereids
    sql 'set enable_nereids_planner=true'
    sql 'set enable_fallback_to_original_planner=false'
    sql 'set runtime_filter_mode=OFF'

    // create tables
    sql """drop table if exists t1;"""
    sql """drop table if exists t2;"""
    sql """drop table if exists t3;"""
    sql """drop table if exists t4;"""

    sql """create table t1 (c1 int, c11 int) distributed by hash(c1) buckets 3 properties('replication_num' = '1');"""
    sql """create table t2 (c2 int, c22 int) distributed by hash(c2) buckets 3 properties('replication_num' = '1');"""
    sql """create table t3 (c3 int, c33 int) distributed by hash(c3) buckets 3 properties('replication_num' = '1');"""
    sql """create table t4 (c4 int, c44 int) distributed by hash(c4) buckets 3 properties('replication_num' = '1');"""

    streamLoad {
        table "t1"
        db "fix_leading"
        set 'column_separator', '|'
        set 'format', 'csv'
        file 't1.csv'
        time 10000
    }

    streamLoad {
        table "t2"
        db "fix_leading"
        set 'column_separator', '|'
        set 'format', 'csv'
        file 't2.csv'
        time 10000
    }

    streamLoad {
        table "t3"
        db "fix_leading"
        set 'column_separator', '|'
        set 'format', 'csv'
        file 't3.csv'
        time 10000
    }

    streamLoad {
        table "t4"
        db "fix_leading"
        set 'column_separator', '|'
        set 'format', 'csv'
        file 't4.csv'
        time 10000
    }

    // bug fix 1: {t1 t2}{t3 t4} miss levels
    qt_select1 """explain shape plan select /*+ leading({t1 t2}{t3 t4}) */ * from t1 join t2 on c2 = c2 join t3 on c1 = c3 join t4 on c1 = c4;"""

    // bug fix 2: fix left outer join without edge with other tables
    // left join + left join
    qt_select2_1_1 """select count(*) from t1 left join t2 on c2 = c2 left join t3 on c2 = c3;"""
    qt_select2_1_2 """select /*+ leading(t1 t2 t3) */ count(*) from t1 left join t2 on c2 = c2 left join t3 on c2 = c3;"""
    qt_select2_1_3 """select /*+ leading(t1 {t2 t3}) */ count(*) from t1 left join t2 on c2 = c2 left join t3 on c2 = c3;"""
    qt_select2_1_4 """select /*+ leading(t1 t3 t2) */ count(*) from t1 left join t2 on c2 = c2 left join t3 on c2 = c3;"""
    qt_select2_1_5 """select /*+ leading(t1 {t2 t2}) */ count(*) from t1 left join t2 on c2 = c2 left join t3 on c2 = c3;"""
    qt_select2_1_6 """select /*+ leading(t2 t1 t3) */ count(*) from t1 left join t2 on c2 = c2 left join t3 on c2 = c3;"""
    qt_select2_1_7 """select /*+ leading(t2 {t1 t3}) */ count(*) from t1 left join t2 on c2 = c2 left join t3 on c2 = c3;"""
    qt_select2_1_8 """select /*+ leading(t2 t3 t1) */ count(*) from t1 left join t2 on c2 = c2 left join t3 on c2 = c3;"""
    qt_select2_1_9 """select /*+ leading(t2 {t3 t1}) */ count(*) from t1 left join t2 on c2 = c2 left join t3 on c2 = c3;"""
    qt_select2_1_10 """select /*+ leading(t3 t1 t2) */ count(*) from t1 left join t2 on c2 = c2 left join t3 on c2 = c3;"""
    qt_select2_1_11 """select /*+ leading(t3 {t1 t2}) */ count(*) from t1 left join t2 on c2 = c2 left join t3 on c2 = c3;"""
    qt_select2_1_12 """select /*+ leading(t3 t2 t1) */ count(*) from t1 left join t2 on c2 = c2 left join t3 on c2 = c3;"""
    qt_select2_1_13 """select /*+ leading(t3 {t2 t1}) */ count(*) from t1 left join t2 on c2 = c2 left join t3 on c2 = c3;"""

    // left join + right join
    qt_select2_2_1 """select count(*) from t1 left join t2 on c2 = c2 right join t3 on c2 = c3;"""
    qt_select2_2_2 """select /*+ leading(t1 t2 t3) */ count(*) from t1 left join t2 on c2 = c2 right join t3 on c2 = c3;"""
    qt_select2_2_3 """select /*+ leading(t1 {t2 t3}) */ count(*) from t1 left join t2 on c2 = c2 right join t3 on c2 = c3;"""
    qt_select2_2_4 """select /*+ leading(t1 t3 t2) */ count(*) from t1 left join t2 on c2 = c2 right join t3 on c2 = c3;"""
    qt_select2_2_5 """select /*+ leading(t1 {t2 t2}) */ count(*) from t1 left join t2 on c2 = c2 right join t3 on c2 = c3;"""
    qt_select2_2_6 """select /*+ leading(t2 t1 t3) */ count(*) from t1 left join t2 on c2 = c2 right join t3 on c2 = c3;"""
    qt_select2_2_7 """select /*+ leading(t2 {t1 t3}) */ count(*) from t1 left join t2 on c2 = c2 right join t3 on c2 = c3;"""
    qt_select2_2_8 """select /*+ leading(t2 t3 t1) */ count(*) from t1 left join t2 on c2 = c2 right join t3 on c2 = c3;"""
    qt_select2_2_9 """select /*+ leading(t2 {t3 t1}) */ count(*) from t1 left join t2 on c2 = c2 right join t3 on c2 = c3;"""
    qt_select2_2_10 """select /*+ leading(t3 t1 t2) */ count(*) from t1 left join t2 on c2 = c2 right join t3 on c2 = c3;"""
    qt_select2_2_11 """select /*+ leading(t3 {t1 t2}) */ count(*) from t1 left join t2 on c2 = c2 right join t3 on c2 = c3;"""
    qt_select2_2_12 """select /*+ leading(t3 t2 t1) */ count(*) from t1 left join t2 on c2 = c2 right join t3 on c2 = c3;"""
    qt_select2_2_13 """select /*+ leading(t3 {t2 t1}) */ count(*) from t1 left join t2 on c2 = c2 right join t3 on c2 = c3;"""

    // left join + semi join
    qt_select2_3_1 """select count(*) from t1 left join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_3_2 """select /*+ leading(t1 t2 t3) */ count(*) from t1 left join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_3_3 """select /*+ leading(t1 {t2 t3}) */ count(*) from t1 left join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_3_4 """select /*+ leading(t1 t3 t2) */ count(*) from t1 left join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_3_5 """select /*+ leading(t1 {t2 t2}) */ count(*) from t1 left join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_3_6 """select /*+ leading(t2 t1 t3) */ count(*) from t1 left join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_3_7 """select /*+ leading(t2 {t1 t3}) */ count(*) from t1 left join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_3_8 """select /*+ leading(t2 t3 t1) */ count(*) from t1 left join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_3_9 """select /*+ leading(t2 {t3 t1}) */ count(*) from t1 left join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_3_10 """select /*+ leading(t3 t1 t2) */ count(*) from t1 left join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_3_11 """select /*+ leading(t3 {t1 t2}) */ count(*) from t1 left join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_3_12 """select /*+ leading(t3 t2 t1) */ count(*) from t1 left join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_3_13 """select /*+ leading(t3 {t2 t1}) */ count(*) from t1 left join t2 on c2 = c2 left semi join t3 on c2 = c3;"""

    // left join + anti join
    qt_select2_4_1 """select count(*) from t1 left join t2 on c2 = c2 left anti join t3 on c2 = c3;"""
    qt_select2_4_2 """select /*+ leading(t1 t2 t3) */ count(*) from t1 left join t2 on c2 = c2 left anti join t3 on c2 = c3;"""
    qt_select2_4_3 """select /*+ leading(t1 {t2 t3}) */ count(*) from t1 left join t2 on c2 = c2 left anti join t3 on c2 = c3;"""
    qt_select2_4_4 """select /*+ leading(t1 t3 t2) */ count(*) from t1 left join t2 on c2 = c2 left anti join t3 on c2 = c3;"""
    qt_select2_4_5 """select /*+ leading(t1 {t2 t2}) */ count(*) from t1 left join t2 on c2 = c2 left anti join t3 on c2 = c3;"""
    qt_select2_4_6 """select /*+ leading(t2 t1 t3) */ count(*) from t1 left join t2 on c2 = c2 left anti join t3 on c2 = c3;"""
    qt_select2_4_7 """select /*+ leading(t2 {t1 t3}) */ count(*) from t1 left join t2 on c2 = c2 left anti join t3 on c2 = c3;"""
    qt_select2_4_8 """select /*+ leading(t2 t3 t1) */ count(*) from t1 left join t2 on c2 = c2 left anti join t3 on c2 = c3;"""
    qt_select2_4_9 """select /*+ leading(t2 {t3 t1}) */ count(*) from t1 left join t2 on c2 = c2 left anti join t3 on c2 = c3;"""
    qt_select2_4_10 """select /*+ leading(t3 t1 t2) */ count(*) from t1 left join t2 on c2 = c2 left anti join t3 on c2 = c3;"""
    qt_select2_4_11 """select /*+ leading(t3 {t1 t2}) */ count(*) from t1 left join t2 on c2 = c2 left anti join t3 on c2 = c3;"""
    qt_select2_4_12 """select /*+ leading(t3 t2 t1) */ count(*) from t1 left join t2 on c2 = c2 left anti join t3 on c2 = c3;"""
    qt_select2_4_13 """select /*+ leading(t3 {t2 t1}) */ count(*) from t1 left join t2 on c2 = c2 left anti join t3 on c2 = c3;"""

    // right join + semi join
    qt_select2_5_1 """select count(*) from t1 right join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_5_2 """select /*+ leading(t1 t2 t3) */ count(*) from t1 right join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_5_3 """select /*+ leading(t1 {t2 t3}) */ count(*) from t1 right join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_5_4 """select /*+ leading(t1 t3 t2) */ count(*) from t1 right join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_5_5 """select /*+ leading(t1 {t2 t2}) */ count(*) from t1 right join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_5_6 """select /*+ leading(t2 t1 t3) */ count(*) from t1 right join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_5_7 """select /*+ leading(t2 {t1 t3}) */ count(*) from t1 right join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_5_8 """select /*+ leading(t2 t3 t1) */ count(*) from t1 right join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_5_9 """select /*+ leading(t2 {t3 t1}) */ count(*) from t1 right join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_5_10 """select /*+ leading(t3 t1 t2) */ count(*) from t1 right join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_5_11 """select /*+ leading(t3 {t1 t2}) */ count(*) from t1 right join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_5_12 """select /*+ leading(t3 t2 t1) */ count(*) from t1 right join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
    qt_select2_5_13 """select /*+ leading(t3 {t2 t1}) */ count(*) from t1 right join t2 on c2 = c2 left semi join t3 on c2 = c3;"""
}
