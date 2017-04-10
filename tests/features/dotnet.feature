Feature: Test of .NET handler

  Scenario: Succesfuly test sln file
    Given We have mercator installed
    When Scanning the dll file
    Then We have correct output
