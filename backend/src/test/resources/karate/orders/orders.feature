Feature: Order Controller API Tests

  Background:
    * url baseUrl
    * header Content-Type = 'application/json'
    * configure retry = { count: 5, interval: 1000 }

  Scenario: Create an order successfully
    # First create a product with enough stock
    Given url baseUrl + '/api/products'
    And request { name: 'Order Test Product', sku: 'OTP-001', price: 25.00, stock: 200 }
    When method post
    Then status 201
    * def productId = response.id

    # Create order (retry due to 10% random payment failure)
    Given url baseUrl + '/api/orders'
    And request { items: [{ productId: '#(productId)', quantity: 2 }], paymentMethod: 'stripe' }
    And retry until responseStatus == 201
    When method post
    Then status 201
    And match response.orderId == '#number'
    And match response.status == 'completed'
    And match response.paymentMethod == 'stripe'
    And match response.totalAmount == 50.00
    And match response.items == '#[1]'
    And match response.items[0].productId == productId
    And match response.items[0].quantity == 2
    And match response.items[0].unitPrice == 25.00
    And match response.items[0].subtotal == 50.00
    And match response.createdAt == '#notnull'

  Scenario: Get order by ID
    # Create a product
    Given url baseUrl + '/api/products'
    And request { name: 'Get Order Product', sku: 'GOP-001', price: 15.00, stock: 200 }
    When method post
    Then status 201
    * def productId = response.id

    # Create order
    Given url baseUrl + '/api/orders'
    And request { items: [{ productId: '#(productId)', quantity: 1 }], paymentMethod: 'stripe' }
    And retry until responseStatus == 201
    When method post
    Then status 201
    * def orderId = response.orderId

    # Get order by ID
    Given url baseUrl + '/api/orders/' + orderId
    When method get
    Then status 200
    And match response.orderId == orderId
    And match response.status == 'completed'
    And match response.items == '#[1]'

  Scenario: Get non-existent order returns 404
    Given url baseUrl + '/api/orders/999999'
    When method get
    Then status 404
    And match response.error == 'Not Found'

  Scenario: Create order with empty items returns 400
    Given url baseUrl + '/api/orders'
    And request { items: [], paymentMethod: 'stripe' }
    When method post
    Then status 400

  Scenario: Create order with missing payment method returns 400
    # Create a product
    Given url baseUrl + '/api/products'
    And request { name: 'No Payment Product', sku: 'NPP-001', price: 10.00, stock: 50 }
    When method post
    Then status 201
    * def productId = response.id

    # Try to create order without payment method
    Given url baseUrl + '/api/orders'
    And request { items: [{ productId: '#(productId)', quantity: 1 }] }
    When method post
    Then status 400

  Scenario: Create order with unsupported payment method returns 400
    # Create a product
    Given url baseUrl + '/api/products'
    And request { name: 'Bad Payment Product', sku: 'BPP-001', price: 10.00, stock: 50 }
    When method post
    Then status 201
    * def productId = response.id

    # Try to create order with unsupported payment method
    Given url baseUrl + '/api/orders'
    And request { items: [{ productId: '#(productId)', quantity: 1 }], paymentMethod: 'bitcoin' }
    When method post
    Then status 400
    And match response.error == 'Bad Request'

  Scenario: Create order with non-existent product returns 404
    Given url baseUrl + '/api/orders'
    And request { items: [{ productId: 999999, quantity: 1 }], paymentMethod: 'stripe' }
    When method post
    Then status 404

  Scenario: Create order with multiple items
    # Create two products
    Given url baseUrl + '/api/products'
    And request { name: 'Multi Item A', sku: 'MIA-001', price: 10.00, stock: 200 }
    When method post
    Then status 201
    * def productIdA = response.id

    Given url baseUrl + '/api/products'
    And request { name: 'Multi Item B', sku: 'MIB-001', price: 20.00, stock: 200 }
    When method post
    Then status 201
    * def productIdB = response.id

    # Create order with multiple items (retry for payment)
    Given url baseUrl + '/api/orders'
    And request { items: [{ productId: '#(productIdA)', quantity: 3 }, { productId: '#(productIdB)', quantity: 2 }], paymentMethod: 'paypal' }
    And retry until responseStatus == 201
    When method post
    Then status 201
    And match response.totalAmount == 70.00
    And match response.items == '#[2]'
    And match response.paymentMethod == 'paypal'

  Scenario: Create order with insufficient stock returns 422
    # Create a product with very low stock
    Given url baseUrl + '/api/products'
    And request { name: 'Low Stock Product', sku: 'LSP-001', price: 5.00, stock: 1 }
    When method post
    Then status 201
    * def productId = response.id

    # Try to order more than available
    Given url baseUrl + '/api/orders'
    And request { items: [{ productId: '#(productId)', quantity: 100 }], paymentMethod: 'stripe' }
    When method post
    Then status 422
    And match response.error == 'Insufficient Stock'

  Scenario: Order with mercadopago payment method
    # Create a product
    Given url baseUrl + '/api/products'
    And request { name: 'MercadoPago Product', sku: 'MP-001', price: 30.00, stock: 200 }
    When method post
    Then status 201
    * def productId = response.id

    # Create order with mercadopago
    Given url baseUrl + '/api/orders'
    And request { items: [{ productId: '#(productId)', quantity: 1 }], paymentMethod: 'mercadopago' }
    And retry until responseStatus == 201
    When method post
    Then status 201
    And match response.paymentMethod == 'mercadopago'
    And match response.status == 'completed'
