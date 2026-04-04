-- V7__Allow_Create_Requests_Without_FoodStall.sql
--
-- Allow CREATE_PENDING registration requests to exist without a FoodStall row.
-- FoodStall will be created only when admin approves.

ALTER TABLE food_stall_updates
    ALTER COLUMN food_stall_id DROP NOT NULL;

