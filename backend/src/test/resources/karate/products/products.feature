Feature: Product Controller API Tests

  Background:
    * url baseUrl + '/api/products'
    * header Content-Type = 'application/json'

  Scenario: Create a product successfully
    Given request { name: 'Wireless Mouse', sku: 'WM-001', description: 'Ergonomic wireless mouse', categoryName: 'Electronics', price: 29.99, stock: 100, weightKg: 0.15 }
    When method post
    Then status 201
    And match response.id == '#number'
    And match response.name == 'Wireless Mouse'
    And match response.sku == 'WM-001'
    And match response.description == 'Ergonomic wireless mouse'
    And match response.categoryName == 'Electronics'
    And match response.price == 29.99
    And match response.stock == 100
    And match response.weightKg == 0.15
    And match response.createdAt == '#notnull'

  Scenario: Get product by ID
    # Create a product first
    Given request { name: 'Keyboard', sku: 'KB-001', price: 49.99, stock: 50 }
    When method post
    Then status 201
    * def productId = response.id

    # Get it by ID
    Given url baseUrl + '/api/products/' + productId
    When method get
    Then status 200
    And match response.id == productId
    And match response.name == 'Keyboard'
    And match response.sku == 'KB-001'

  Scenario: List all products with pagination
    # Create two products
    Given request { name: 'Product A', sku: 'PA-001', price: 10.00, stock: 5 }
    When method post
    Then status 201

    Given url baseUrl + '/api/products'
    And request { name: 'Product B', sku: 'PB-001', price: 20.00, stock: 10 }
    When method post
    Then status 201

    # List with pagination
    Given url baseUrl + '/api/products'
    And param page = 0
    And param size = 10
    When method get
    Then status 200
    And match response.content == '#array'
    And match response.totalElements == '#number'
    And match response.number == 0
    And match response.size == 10

  Scenario: Update a product
    # Create a product
    Given request { name: 'Old Name', sku: 'UP-001', price: 15.00, stock: 30 }
    When method post
    Then status 201
    * def productId = response.id

    # Update it
    Given url baseUrl + '/api/products/' + productId
    And request { name: 'New Name', sku: 'UP-001', description: 'Updated description', price: 25.00, stock: 50 }
    When method put
    Then status 200
    And match response.id == productId
    And match response.name == 'New Name'
    And match response.description == 'Updated description'
    And match response.price == 25.00
    And match response.stock == 50

  Scenario: Delete a product
    # Create a product
    Given request { name: 'To Delete', sku: 'DEL-001', price: 5.00, stock: 1 }
    When method post
    Then status 201
    * def productId = response.id

    # Delete it
    Given url baseUrl + '/api/products/' + productId
    When method delete
    Then status 204

    # Verify it's gone
    Given url baseUrl + '/api/products/' + productId
    When method get
    Then status 404
    And match response.error == 'Not Found'

  Scenario: Search products by name
    # Create a product with a distinctive name
    Given request { name: 'Unique Searchable Widget', sku: 'SW-001', price: 99.99, stock: 10 }
    When method post
    Then status 201

    # Search for it
    Given url baseUrl + '/api/products/search'
    And param q = 'Searchable'
    And param page = 0
    And param size = 10
    When method get
    Then status 200
    And match response.content == '#array'
    And match response.content[0].name contains 'Searchable'

  Scenario: Create product with missing required fields returns 400
    Given request { description: 'No name or sku' }
    When method post
    Then status 400
    And match response.status == 400
    And match response.error == 'Bad Request'
    And match response.errors == '#notnull'

  Scenario: Create product with duplicate SKU returns 409
    # Create first product
    Given request { name: 'First Product', sku: 'DUP-SKU', price: 10.00, stock: 5 }
    When method post
    Then status 201

    # Try to create another with same SKU
    Given url baseUrl + '/api/products'
    And request { name: 'Second Product', sku: 'DUP-SKU', price: 20.00, stock: 10 }
    When method post
    Then status 409
    And match response.error == '#notnull'

  Scenario: Get non-existent product returns 404
    Given url baseUrl + '/api/products/999999'
    When method get
    Then status 404
    And match response.status == 404
    And match response.error == 'Not Found'

  Scenario: Create product with negative price returns 400
    Given request { name: 'Bad Price', sku: 'BP-001', price: -5.00, stock: 10 }
    When method post
    Then status 400
    And match response.errors == '#notnull'

  Scenario: List products with sorting
    # Create products
    Given request { name: 'Zebra Product', sku: 'ZP-001', price: 100.00, stock: 1 }
    When method post
    Then status 201

    Given url baseUrl + '/api/products'
    And request { name: 'Alpha Product', sku: 'AP-001', price: 50.00, stock: 2 }
    When method post
    Then status 201

    # List sorted by name ascending
    Given url baseUrl + '/api/products'
    And param sortBy = 'name'
    And param sortDir = 'asc'
    And param size = 100
    When method get
    Then status 200
    And match response.content == '#array'

  Scenario: Update non-existent product returns 404
    Given url baseUrl + '/api/products/999999'
    And request { name: 'Ghost', sku: 'GHOST-001', price: 10.00, stock: 1 }
    When method put
    Then status 404
