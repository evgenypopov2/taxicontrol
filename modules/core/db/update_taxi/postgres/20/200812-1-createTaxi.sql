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
);