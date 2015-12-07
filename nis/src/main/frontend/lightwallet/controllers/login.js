'use strict';
define([
    'definitions',
    'utils/Address',
    'utils/CryptoHelpers',
    'utils/KeyPair',
    'utils/NodeConnector',
    'services/NetworkData'
], function(angular, Address, CryptoHelpers, KeyPair, NodeConnector) {
    var mod = angular.module('walletApp.controllers');

	mod.controller('LoginCtrl', ["$scope", "$localStorage", "$timeout", "networkData",
	        function($scope, $localStorage, $timeout, networkData) {

        $scope.$on('$locationChangeStart', function( event ) {
            if ($scope.connector) {
                $scope.connector.close();
            }
        });
        $scope.connector = undefined;
        $scope.$storage = $localStorage.$default({});
        $scope.connectionStatus = "connecting";
        $scope.showAll = false;

        // fix for old testnet accounts
        $.each($scope.$storage.wallets || [], function fixOldWallets(idx, e) {
            if (e.accounts[0].network === undefined) {
                e.accounts[0].network = -104;
            }
        });
        var connector = NodeConnector();
        connector.connect(function(){
            $scope.$apply(function(){
                $scope.connectionStatus = "checking";
            });

            connector.on('errors', function(name, d) {
                console.log(d);
                alert(d);
            });
            connector.on('nodeInfo', function(d) {
                $scope.$apply(function(){
                    $scope.connectionStatus = "connected";
                    $scope.showAll = true;
                    $scope.network = d.metaData.networkId;
                    $scope.nisPort = d.endpoint.port;

                    networkData.setNetworkId($scope.network);
                    networkData.setNisPort($scope.nisPort);
                });
            });
            connector.requestNodeInfo();
        });

        $scope.filterNetwork = function filterNetwork(elem) {
            return elem.accounts[0].network == $scope.network;
        };

        $scope.addWalletHidden = true;
        $scope.addSaltedWalletHidden = true;
        $scope.addPassWalletHidden = $scope.$storage.wallets !== undefined;

        $scope.generatingInProgress = false;
        $scope.addSaltedWalletButtonText = "Create";
        $scope.addPassWalletButtonText = "Create";

        $scope.hideAll = function() {
            $scope.addWalletHidden = true;
            $scope.addSaltedWalletHidden = true;
            $scope.addPassWalletHidden = true;
        };
        $scope.showAddWallet = function() {
            $scope.hideAll();
            $scope.addWalletHidden = false;
        };
        $scope.showaddSaltedWallet = function() {
            $scope.hideAll();
            $scope.addSaltedWalletHidden = false;
        };
        $scope.showAddPassWallet = function() {
            $scope.hideAll();
            $scope.addPassWalletHidden = false;
        };

        $scope.addWallet = function()
        {
            $scope.dummy.accounts[0].brain = false;
            $scope.dummy.accounts[0].network = $scope.network;
            $localStorage.wallets = ($localStorage.wallets || []).concat($scope.dummy);
            $scope.dummy = undefined;
            $scope.hideAll();
        };

        $scope.addSaltedWallet = function()
        {
            $scope.generatingInProgress = true;
            $scope.addSaltedWalletButtonText = "Generating";
            $timeout(function() {
                var r = CryptoHelpers.generateSaltedKey($scope.dummy.accounts[0].password);
                var k = KeyPair.create(r.priv);
                var addr = Address.toAddress(k.publicKey.toString(), $scope.network);
                $scope.dummy.accounts[0].brain = true;
                $scope.dummy.accounts[0].algo = "pbkf2:1k";
                $scope.dummy.accounts[0].salt = r.salt;
                $scope.dummy.accounts[0].address = addr;
                $scope.dummy.accounts[0].network = $scope.network;
                delete $scope.dummy.accounts[0].password;

                $localStorage.wallets = ($localStorage.wallets || []).concat($scope.dummy);
                $scope.dummy = undefined;
                $scope.hideAll();
                $scope.generatingInProgress = false;
                $scope.addSaltedWalletButtonText = "Create";
            }, 500);
        };


        $scope.addPassWallet = function()
        {
            $scope.generatingInProgress = true;
            $scope.addPassWalletButtonText = "Generating";
            $timeout(function() {
                var r = CryptoHelpers.derivePassSha($scope.dummy.accounts[0].password, 6000);
                var k = KeyPair.create(r.priv);
                var addr = Address.toAddress(k.publicKey.toString(), $scope.network);
                $scope.dummy.accounts[0].brain = true;
                $scope.dummy.accounts[0].algo = "pass:6k";
                $scope.dummy.accounts[0].salt = '';
                $scope.dummy.accounts[0].address = addr;
                $scope.dummy.accounts[0].network = $scope.network;
                delete $scope.dummy.accounts[0].password;

                $localStorage.wallets = ($localStorage.wallets || []).concat($scope.dummy);
                $scope.dummy = undefined;
                $scope.hideAll();
                $scope.generatingInProgress = false;
                $scope.addSaltedWalletButtonText = "Create";
            }, 500);
        };
	}]);

    /**
     * This directive does not use isolated scope, so we will have access to scope.network
     */
    mod.directive('address', function() {
        return {
            require: 'ngModel',
            link: function(scope, elm, attrs, ctrl) {
                ctrl.$validators.address = function(modelValue, viewValue) {
                    if (ctrl.$isEmpty(modelValue)) {
                        return false;
                    }
                    return Address.isValid(modelValue);
                };
            }
        };
    });

    return mod;
});