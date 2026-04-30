Street Voice Database Schema
Generated from current Flyway migrations and running PostgreSQL schema.
Data rows are intentionally not included.

Applied migrations
==================
V1__Init_Schema.sql
V2__Fix_FoodStall_Schema.sql
V3__Add_Localization.sql
V4__Add_Priority_To_FoodStall.sql
V5__Drop_ActionType_Check.sql
V6__Migrate_AdminUsers_To_System_Users.sql
V7__Allow_Create_Requests_Without_FoodStall.sql
V8__Seed_Initial_Data.sql
V9__Allow_Idle_UserActivity_Without_FoodStall.sql

Tables and columns
==================

Table: admin_users
------------------
id                       bigint                      NOT NULL DEFAULT nextval('admin_users_id_seq'::regclass)
created_at               timestamp without time zone NOT NULL
email                    character varying           NOT NULL
enabled                  boolean                     NOT NULL
oauth_provider           character varying           NULL
oauth_provider_user_id   character varying           NULL
password_hash            character varying           NULL
refresh_token_expires_at timestamp without time zone NULL
refresh_token_hash       character varying           NULL
role                     character varying           NOT NULL
updated_at               timestamp without time zone NOT NULL
username                 character varying           NOT NULL

Table: flyway_schema_history
----------------------------
installed_rank integer                     NOT NULL
version        character varying           NULL
description    character varying           NOT NULL
type           character varying           NOT NULL
script         character varying           NOT NULL
checksum       integer                     NULL
installed_by   character varying           NOT NULL
installed_on   timestamp without time zone NOT NULL DEFAULT now()
execution_time integer                     NOT NULL
success        boolean                     NOT NULL

Table: food_stall_localizations
-------------------------------
id             bigint                      NOT NULL DEFAULT nextval('food_stall_localizations_id_seq'::regclass)
food_stall_id  bigint                      NOT NULL
language_code  character varying           NOT NULL
name           character varying           NULL
description    text                        NULL
audio_url      character varying           NULL
created_at     timestamp without time zone NULL DEFAULT CURRENT_TIMESTAMP
address        character varying           NULL

Table: food_stall_updates
-------------------------
id            bigint                      NOT NULL DEFAULT nextval('food_stall_updates_id_seq'::regclass)
food_stall_id bigint                      NULL
owner_id      bigint                      NULL
status        character varying           NULL DEFAULT 'PENDING'::character varying
changes       jsonb                       NOT NULL
reason        character varying           NULL
created_at    timestamp without time zone NULL DEFAULT CURRENT_TIMESTAMP
reviewed_at   timestamp without time zone NULL
reviewed_by   bigint                      NULL

Table: food_stalls
------------------
id                  bigint                      NOT NULL DEFAULT nextval('food_stalls_id_seq'::regclass)
name                character varying           NOT NULL
address             character varying           NULL
description         text                        NULL
audio_url           character varying           NULL
image_url           character varying           NULL
location            geography                   NULL
trigger_radius      integer                     NULL DEFAULT 15
min_price           integer                     NULL
max_price           integer                     NULL
audio_duration      integer                     NULL
featured_reviews    jsonb                       NULL
rating              double precision            NULL
created_at          timestamp without time zone NULL DEFAULT CURRENT_TIMESTAMP
priority            integer                     NULL DEFAULT 0
status              character varying           NULL DEFAULT 'ACTIVE'::character varying
owner_id            bigint                      NULL
updated_at          timestamp without time zone NULL DEFAULT CURRENT_TIMESTAMP
localization_status character varying           NULL

Table: user_activities
----------------------
id               bigint                      NOT NULL DEFAULT nextval('user_activities_id_seq'::regclass)
device_id        character varying           NOT NULL
food_stall_id    bigint                      NULL
action_type      character varying           NOT NULL
duration_seconds integer                     NULL
created_at       timestamp without time zone NULL DEFAULT CURRENT_TIMESTAMP
event_time       timestamp without time zone NULL
platform         character varying           NULL
session_id       character varying           NULL

Table: users
------------
id                       bigint                      NOT NULL DEFAULT nextval('users_id_seq'::regclass)
username                 character varying           NOT NULL
email                    character varying           NOT NULL
password_hash            character varying           NOT NULL
oauth_provider           character varying           NULL
oauth_subject            character varying           NULL
restaurant_id            bigint                      NULL
role                     character varying           NULL DEFAULT 'ADMIN'::character varying
enabled                  boolean                     NULL DEFAULT true
created_at               timestamp without time zone NULL DEFAULT CURRENT_TIMESTAMP
updated_at               timestamp without time zone NULL DEFAULT CURRENT_TIMESTAMP
oauth_provider_user_id   character varying           NULL
refresh_token_expires_at timestamp without time zone NULL
refresh_token_hash       character varying           NULL

Foreign-key columns
===================
food_stall_localizations.food_stall_id -> food_stalls.id
food_stall_updates.food_stall_id       -> food_stalls.id
food_stall_updates.owner_id            -> users.id
food_stall_updates.reviewed_by         -> users.id
food_stalls.owner_id                   -> users.id
user_activities.food_stall_id          -> food_stalls.id

Important indexes
=================
idx_food_stalls_location
idx_localization_stall_lang
idx_users_restaurant_id
idx_users_role
idx_food_stalls_status
idx_food_stalls_owner_id
idx_food_stall_updates_status
idx_food_stall_updates_owner_id
