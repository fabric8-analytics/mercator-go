Feature: Test of Go-Glide handler

  Scenario: Successfully test Go Glide files
    Given We have mercator installed
    When Scanning the Go Glide files
    Then We have correct output

  Scenario: Successfully test Go Dep files
    Given We have mercator installed
    When Scanning the Go Pkg files
    Then We have correct output
