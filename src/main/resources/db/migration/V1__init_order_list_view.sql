create table if not exists order_list_view
(
    order_id                       bigint        not null,
    order_number                   varchar(64),
    order_status                   varchar(32),
    order_amount                   decimal(19, 2),
    ordered_at                     datetime(6),
    order_section_updated_at       datetime(6),

    member_id                      bigint,
    member_name                    varchar(128),
    member_email                   varchar(256),
    member_grade                   varchar(32),
    member_section_updated_at      datetime(6),

    product_id                     bigint,
    product_name                   varchar(256),
    product_category               varchar(64),
    product_price                  decimal(19, 2),
    product_section_updated_at     datetime(6),

    delivery_status                varchar(32),
    delivery_address               varchar(512),
    delivery_tracked_at            datetime(6),
    delivery_section_updated_at    datetime(6),

    created_at                     datetime(6),
    updated_at                     datetime(6),

    primary key (order_id),
    key idx_olv_member_id (member_id),
    key idx_olv_product_id (product_id),
    key idx_olv_ordered_at (ordered_at)
) engine = InnoDB
  default charset = utf8mb4
  collate = utf8mb4_unicode_ci;
