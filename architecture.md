flowchart TD

subgraph group_access["Access &amp; Profiles"]
  node_auth_api["Auth API"]
  node_auth_service["Auth Service<br/>[AuthService.java]"]
  node_jwt_security["JWT Security"]
  node_customer_api["Customer API"]
  node_customer_service["Customer Service"]
  node_user_store[("User Store")]
  node_address_store[("Address Store")]
end

subgraph group_orders["Order Operations"]
  node_order_api["Order API"]
  node_order_service["Order Service<br/>[OrderService.java]"]
  node_pricing_service["Pricing Service"]
  node_cancellation_service["Cancellation Service"]
  node_order_store[("Order Store")]
end

subgraph group_fulfillment["Batch Fulfillment"]
  node_batch_engine["Batching Engine"]
  node_batch_store[("Batch Store")]
  node_stop_store[("Stop Store")]
  node_driver_ops["Driver Batch API"]
  node_driver_service["Driver Service<br/>[DriverService.java]"]
  node_driver_store[("Driver Store")]
  node_payout_service["Payout Service"]
end

subgraph group_platform["Platform Services"]
  node_config_store[("Config Store")]
  node_notification_service["Notification Service"]
  node_notification_store[("Notification Store")]
end

subgraph group_admin["Administration"]
  node_admin_api["Admin API"]
  node_admin_store[("Admin Store")]
end

node_customer_actor(("Customer"))
node_driver_actor(("Driver"))
node_admin_actor(("Administrator"))
node_fcm_service["FCM Push"]

node_customer_actor -->|"requests OTP"| node_auth_api
node_driver_actor -->|"authenticates"| node_auth_api
node_admin_actor -->|"admin login"| node_auth_api
node_auth_api -->|"handles auth"| node_auth_service
node_auth_service -->|"loads users"| node_user_store
node_auth_service -->|"loads drivers"| node_driver_store
node_auth_service -->|"loads admins"| node_admin_store
node_auth_service -->|"issues tokens"| node_jwt_security
node_customer_actor -->|"manages profile"| node_customer_api
node_customer_api -->|"dispatches requests"| node_customer_service
node_customer_service -->|"reads users"| node_user_store
node_customer_service -->|"manages addresses"| node_address_store
node_customer_actor -->|"places orders"| node_order_api
node_order_api -->|"calculates fare"| node_pricing_service
node_order_api -->|"creates orders"| node_order_service
node_order_api -->|"cancels orders"| node_cancellation_service
node_order_service -->|"prices order"| node_pricing_service
node_order_service -->|"saves orders"| node_order_store
node_order_service -->|"notifies customer"| node_notification_service
node_cancellation_service -->|"updates orders"| node_order_store
node_cancellation_service -->|"cancels stops"| node_stop_store
node_cancellation_service -->|"reads strategy"| node_config_store
node_cancellation_service -->|"notifies cancellation"| node_notification_service
node_batch_engine -->|"reads thresholds"| node_config_store
node_batch_engine -->|"finds open orders"| node_order_store
node_batch_engine -->|"saves batches"| node_batch_store
node_batch_engine -->|"creates stops"| node_stop_store
node_driver_actor -->|"updates availability"| node_driver_service
node_driver_service -->|"updates driver"| node_driver_store
node_driver_actor -->|"executes stops"| node_driver_ops
node_driver_ops -->|"reads batches"| node_batch_store
node_driver_ops -->|"completes stops"| node_stop_store
node_driver_ops -->|"updates orders"| node_order_store
node_payout_service -->|"reads payout config"| node_config_store
node_payout_service -->|"records payout"| node_batch_store
node_payout_service -->|"counts drops"| node_stop_store
node_payout_service -->|"totals order value"| node_order_store
node_notification_service -->|"saves notifications"| node_notification_store
node_notification_service -->|"finds user token"| node_user_store
node_notification_service -->|"finds driver token"| node_driver_store
node_notification_service -.->|"sends push"| node_fcm_service
node_admin_actor -->|"manages operations"| node_admin_api
node_admin_api -->|"forms batches"| node_batch_engine
node_admin_api -->|"manages config"| node_config_store
node_admin_api -->|"reviews orders"| node_order_store
node_admin_api -->|"reviews drivers"| node_driver_store
node_admin_api -->|"reviews batches"| node_batch_store

click node_auth_api "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/auth/AuthController.java"
click node_auth_service "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/auth/AuthService.java"
click node_jwt_security "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/common/JwtAuthenticationFilter.java"
click node_customer_api "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/customer/CustomerController.java"
click node_customer_service "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/customer/service/CustomerService.java"
click node_user_store "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/customer/repository/UserRepository.java"
click node_address_store "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/customer/repository/AddressRepository.java"
click node_order_api "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/order/OrderController.java"
click node_order_service "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/order/service/OrderService.java"
click node_pricing_service "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/order/service/PricingService.java"
click node_cancellation_service "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/order/service/OrderCancellationService.java"
click node_order_store "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/order/repository/OrderRepository.java"
click node_batch_engine "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/batch/service/BatchingEngine.java"
click node_batch_store "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/batch/repository/BatchRepository.java"
click node_stop_store "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/batch/repository/BatchStopRepository.java"
click node_driver_ops "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/driver/DriverBatchController.java"
click node_driver_service "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/driver/service/DriverService.java"
click node_driver_store "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/driver/repository/DriverRepository.java"
click node_payout_service "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/config/service/DriverPayoutService.java"
click node_config_store "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/config/repository/SystemConfigRepository.java"
click node_notification_service "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/notification/service/NotificationService.java"
click node_notification_store "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/notification/repository/NotificationRepository.java"
click node_admin_api "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/admin/AdminController.java"
click node_admin_store "https://github.com/jay-6957/pahadcarry-backend/blob/main/src/main/java/com/pahadcarry/admin/repository/AdminUserRepository.java"

classDef toneNeutral fill:#f8fafc,stroke:#334155,stroke-width:1.5px,color:#0f172a
classDef toneBlue fill:#dbeafe,stroke:#2563eb,stroke-width:1.5px,color:#172554
classDef toneAmber fill:#fef3c7,stroke:#d97706,stroke-width:1.5px,color:#78350f
classDef toneMint fill:#dcfce7,stroke:#16a34a,stroke-width:1.5px,color:#14532d
classDef toneRose fill:#ffe4e6,stroke:#e11d48,stroke-width:1.5px,color:#881337
classDef toneIndigo fill:#e0e7ff,stroke:#4f46e5,stroke-width:1.5px,color:#312e81
classDef toneTeal fill:#ccfbf1,stroke:#0f766e,stroke-width:1.5px,color:#134e4a
class node_auth_api,node_auth_service,node_jwt_security,node_customer_api,node_customer_service,node_user_store,node_address_store toneBlue
class node_order_api,node_order_service,node_pricing_service,node_cancellation_service,node_order_store toneAmber
class node_batch_engine,node_batch_store,node_stop_store,node_driver_ops,node_driver_service,node_driver_store,node_payout_service toneMint
class node_config_store,node_notification_service,node_notification_store toneRose
class node_admin_api,node_admin_store,node_customer_actor,node_driver_actor,node_admin_actor,node_fcm_service toneIndigo
