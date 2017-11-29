Feature: Test of Go-Glide handler

  Scenario: Succesfuly test Go Glide files
    Given We have mercator installed
    When Scanning the Go Glide files
    Then We have correct output
