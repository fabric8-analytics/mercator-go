Feature: Test of Python handler

  Scenario: Succesfuly test setup.py file
    Given We have mercator installed
    When Scanning the setup.py file
    Then We have correct output
