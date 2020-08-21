-- begin TAXICONTROL_TAXI
create table TAXICONTROL_TAXI (
    ID uuid,
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
-- begin TAXICONTROL_AUTO_MODEL
create table TAXICONTROL_AUTO_MODEL (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255),
    VENDOR_ID uuid,
    TYPE_ varchar(50),
    --
    primary key (ID)
)^
-- end TAXICONTROL_AUTO_MODEL
-- begin TAXICONTROL_AUTO_VENDOR
create table TAXICONTROL_AUTO_VENDOR (
    ID uuid,
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
