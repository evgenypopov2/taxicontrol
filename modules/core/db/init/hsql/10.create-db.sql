-- begin TAXICONTROL_ROUTE_COST
create table TAXICONTROL_ROUTE_COST (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    TAXI_TYPE varchar(50),
    COST_PER_ONE integer,
    --
    primary key (ID)
)^
-- end TAXICONTROL_ROUTE_COST
-- begin TAXICONTROL_TAXI
create table TAXICONTROL_TAXI (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    DRIVER_NAME varchar(255),
    DRIVER_PHONE varchar(255) not null,
    NUMBER_ varchar(255),
    --
    primary key (ID)
)^
-- end TAXICONTROL_TAXI
-- begin TAXICONTROL_CLIENT
create table TAXICONTROL_CLIENT (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255),
    PHONE varchar(255),
    START_POINT VARCHAR(100),
    END_POINT VARCHAR(100),
    --
    primary key (ID)
)^
-- end TAXICONTROL_CLIENT
-- begin TAXICONTROL_AUTO_MODEL
create table TAXICONTROL_AUTO_MODEL (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255),
    VENDOR_ID varchar(36),
    TYPE_ varchar(50),
    --
    primary key (ID)
)^
-- end TAXICONTROL_AUTO_MODEL
-- begin TAXICONTROL_ROUTE
create table TAXICONTROL_ROUTE (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    TAXI_ID varchar(36),
    PATH VARCHAR(4000),
    ORDER_TIME timestamp,
    START_TIME timestamp,
    END_TIME timestamp,
    COST integer,
    --
    primary key (ID)
)^
-- end TAXICONTROL_ROUTE
-- begin TAXICONTROL_AUTO_VENDOR
create table TAXICONTROL_AUTO_VENDOR (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255),
    --
    primary key (ID)
)^
-- end TAXICONTROL_AUTO_VENDOR
