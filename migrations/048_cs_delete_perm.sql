-- ============================================================================
-- migration 048: CS conversation hard-delete permission
--
-- Why this exists
--   Sprint 49.5 hotfix. Operations team flagged that test / spam / mis-
--   routed conversations from the WebWidget and Email channels accumulate
--   in the inbox forever. Chatwoot does not auto-purge anything, and the
--   "delete conversation" REST endpoint only lives behind admin auth on
--   the Chatwoot side -- it is not surfaced to AgriOS agents at all.
--
--   This migration seeds the `cs:conversation:delete` permission and binds
--   it ONLY to SUPER_ADMIN. Regular agents (and even MANAGER / LEADER)
--   cannot delete conversations -- they can still resolve / archive via
--   the normal status workflow, which is reversible.
--
-- What it does
--   1. Registers a hidden "button" menu row carrying the perm string.
--      Hidden because there is no navigable page behind it; the perm is
--      consumed by the delete button rendered inside ConversationDetail.
--   2. Binds the row to the SUPER_ADMIN role only.
--
-- Idempotent
--   Both statements use INSERT IGNORE so re-running the migration on a
--   database that already has them is a no-op.
-- ============================================================================

INSERT IGNORE INTO sys_menu
    (id, parent_id, code,                   name,                 type,    path, perms,                     sort, visible)
VALUES
    (998, 0,        'cs.conversation.delete', 'Delete conversation', 'button', NULL, 'cs:conversation:delete',  99,   0);

-- Bind to SUPER_ADMIN only.
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 998 FROM sys_role r WHERE r.code = 'SUPER_ADMIN';
