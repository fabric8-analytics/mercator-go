Feature: Test of Haskell handler

  Scenario: Succesfuly test Cabal file
    Given We have mercator installed
    When Scanning the Cabal file
    Then We have correct output
