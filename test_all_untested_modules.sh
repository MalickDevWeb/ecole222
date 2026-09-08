#!/bin/bash

# Script principal pour tester tous les modules non testés
# Exécute tous les scripts de test des modules sans tests précédents

echo "=========================================="
echo "🧪 TEST COMPLET DES MODULES NON TESTÉS"
echo "=========================================="
echo "Date: $(date)"
echo "=========================================="

# Variables pour le résumé global
TOTAL_MODULES=0
TOTAL_TESTS=0
TOTAL_PASSED=0
TOTAL_FAILED=0

# Fonction pour exécuter un script de test et collecter les résultats
run_test_script() {
    local script_name=$1
    local module_name=$2
    
    echo -e "\n=========================================="
    echo "🔍 TEST MODULE: $module_name"
    echo "=========================================="
    
    TOTAL_MODULES=$((TOTAL_MODULES + 1))
    
    # Exécuter le script de test
    bash "$script_name"
    local exit_code=$?
    
    # Lire les résultats du fichier de logs
    if [ -f "${module_name,,}_test_results.log" ]; then
        while IFS= read -r line; do
            if [[ $line =~ Total:.*Passed:.*Failed: ]]; then
                echo "$line"
                # Extraire les chiffres
                local total=$(echo "$line" | grep -oP 'Total: \K\d+')
                local passed=$(echo "$line" | grep -oP 'Passed: \K\d+')
                local failed=$(echo "$line" | grep -oP 'Failed: \K\d+')
                
                TOTAL_TESTS=$((TOTAL_TESTS + total))
                TOTAL_PASSED=$((TOTAL_PASSED + passed))
                TOTAL_FAILED=$((TOTAL_FAILED + failed))
            fi
        done < "${module_name,,}_test_results.log"
    fi
    
    return $exit_code
}

# Liste des scripts à exécuter
declare -a scripts=(
    "test_admin_9endpoints.sh:ADMIN"
    "test_dashboard_18endpoints.sh:DASHBOARD"
    "test_finance_19endpoints.sh:FINANCE"
    "test_planning_12endpoints.sh:PLANNING"
    "test_security_12endpoints.sh:SECURITY"
)

# Exécuter chaque script de test
for script_info in "${scripts[@]}"; do
    IFS=':' read -r script_name module_name <<< "$script_info"
    
    if [ -f "$script_name" ]; then
        run_test_script "$script_name" "$module_name"
    else
        echo -e "⚠️  Script non trouvé: $script_name"
    fi
done

# Résumé global
echo -e "\n=========================================="
echo "📊 RÉSUMÉ GLOBAL DES TESTS MODULES NON TESTÉS"
echo "=========================================="
echo "Modules testés: $TOTAL_MODULES"
echo "Total tests: $TOTAL_TESTS"
echo -e "${GREEN}Tests réussis: $TOTAL_PASSED${NC}"
echo -e "${RED}Tests échoués: $TOTAL_FAILED${NC}"
echo "Taux de réussite: $(awk "BEGIN {printf \"%.2f\", ($TOTAL_PASSED/$TOTAL_TESTS)*100}")%"
echo "=========================================="

# Sauvegarde du résumé global
echo "ALL UNTESTED MODULES TEST RESULTS - $(date)" > all_untested_modules_test_results.log
echo "Modules: $TOTAL_MODULES, Total: $TOTAL_TESTS, Passed: $TOTAL_PASSED, Failed: $TOTAL_FAILED" >> all_untested_modules_test_results.log
echo "Success Rate: $(awk "BEGIN {printf \"%.2f\", ($TOTAL_PASSED/$TOTAL_TESTS)*100}")%" >> all_untested_modules_test_results.log

if [ $TOTAL_FAILED -eq 0 ]; then
    echo -e "${GREEN}✅ TOUS LES TESTS DES MODULES NON TESTÉS SONT PASSÉS${NC}"
    exit 0
else
    echo -e "${RED}❌ CERTAINS TESTS DES MODULES NON TESTÉS ONT ÉCHOUÉ${NC}"
    exit 1
fi