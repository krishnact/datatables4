package datatables4

import grails.orm.HibernateCriteriaBuilder
import org.hibernate.Criteria
import org.hibernate.HibernateException
import org.hibernate.criterion.*
import org.hibernate.type.StringType

/**
 * Aggregate query criteria for a Domain Class.
 * This class allows you to create a criteria query that includes associations and SQLRestrictions with proper {alias} substitution.
 * Based loosely on http://blog.serindu.com/2014/02/05/criteria-aggregator-dynamic-criteria-queries-in-grails/
 */
public class CriteriaAggregator {
    private Class forClass
    private List andCriteria = []
    private Map orCriteriaGroups = [:]
    private List orders = []
    private int aliasIndex = 0

    /**
     * The criteriaTree list is the top-level node of a tree that stores the criteria, association names and the join types.
     */
    private CriteriaTreeNode criteriaTree = new CriteriaTreeNode()

    /**
     * @param forClass should be a Grails DomainClass.
     */
    public CriteriaAggregator(Class forClass) {
        this.forClass = forClass;
    }

    /**
     * Gets the alias that should be used when creating criteria. This method also creates the association criteria.
     * @param path The list of association names.
     * @param joinTypes The list of join types.
     * Each item must be one of CriteriaSpecification.INNER_JOIN, CriteriaSpecification.LEFT_JOIN,
     * or CriteriaSpecification.FULL_JOIN.
     * Note that each join can only have one join type, so subsequent invocations of this method can override a join type.
     * To avoid overriding a previously set join type, you can include a null in the joinTypes list. Null values will
     * not override a previously-set join type.
     * @return
     */
    def getAssociationAlias(List path, List joinTypes = null) {
        def alias
        CriteriaTreeNode currentNode = criteriaTree
        path.eachWithIndex { namePart, index ->
            currentNode = getChildNode(currentNode, namePart)
            alias = currentNode.alias
            if(joinTypes) {
                def joinType = joinTypes[index]
                if(null != joinType) {
                    currentNode.joinType = joinType
                }
            }
        }
        alias
    }

    /**
     * Add an AND or OR criterion for a field. If orGroup is specified, then the criterion will be added to that group.
     * @param criterion The hibernate criterion.
     */
    public void addCriterion(Criterion criterion, def orGroup = null) {
        if(orGroup != null) {
            def orList = orCriteriaGroups[orGroup]
            if(!orList) {
                orList = []
                orCriteriaGroups[orGroup] = orList
            }
            orList << criterion
        } else {
            andCriteria << criterion
        }
    }

    /**
     * Creates an AliasSQLCriterion, which is like a hibernate SQLRestriction without the massive alias defect.
     * @param sql The SQL.
     * @param path The list of association names. This is used to determine the alias.
     */
    def getSqlRestriction(def sql, def search, List path) {
        new AliasSQLCriterion(sql.toString(), search, getCriteriaNode(path))
    }

    /**
     * Add an Order to sort the results of the list method.
     * @param order The hibernate Order.
     * @param path The list of association names.
     */
    public void addOrder(Order order) {
        orders << order
    }

    /**
     * Gets the node from the criteriaTree specified by the path.
     * @param path The list of association names in the path.
     */
    private getCriteriaNode(List path) {
        CriteriaTreeNode currentNode = criteriaTree
        if(path) {
            path.each { namePart ->
                currentNode = getChildNode(currentNode, namePart)
            }
        }
        currentNode
    }

    /**
     * Gets the child node from the criteriaTree specified by the association name.
     * @param path The list of association names in the path.
     */
    private getChildNode(CriteriaTreeNode currentNode, def associationName) {
        def childMap = currentNode.children
        def childNode = childMap[associationName]
        if(!childNode) {
            childNode = new CriteriaTreeNode()
            childNode.name = associationName
            childNode.alias = "X" + aliasIndex++ + associationName
            childMap[associationName] = childNode
        }
        childNode
    }

    /**
     * Count the results.
     * @return
     */
    public long count() {
        return runQuery('get') {
            projections {rowCount()}
        }
    }

    /**
     * Not used; not tested.
     * @param additionalCriteria
     * @return
     */
    public def get(Closure additionalCriteria=null) {
        return runQuery('get', additionalCriteria) // Query must return only a single row
    }

    /**
     * Return a list of results in the form of a PagedResultList.
     * @param pagingMap A map containing max and offset values.
     * @param additionalCriteria
     * @return a PagedResultList. Note that calling getTotalCount() on the PagedResultList is unlikely to work.
     */
    public def list(def pagingMap=null, Closure additionalCriteria=null) {
        HibernateCriteriaBuilder criteriaBuilder = forClass.createCriteria()
        criteriaBuilder.list(pagingMap) {
            if(additionalCriteria) {
                additionalCriteria.delegate = criteriaBuilder;
                additionalCriteria()
            }

            def criteriaInstance = criteriaBuilder.getInstance()
            createSubCriteria(criteriaInstance, criteriaTree)

            // Add the OR criteria.
            orCriteriaGroups.each { key, orList ->
                def disjunction = Restrictions.disjunction()
                orList.each {
                    disjunction.add(it)
                }
                criteriaInstance.add(disjunction)
            }

            // Add the AND criteria and the orders.
            andCriteria.each {
                criteriaInstance.add(it)
            }
            orders.each {
                criteriaInstance.addOrder(it.ignoreCase())
            }
        }
    }

    /**
     * Recursive method create the associations and aliases.
     * @param criteria
     * @param criteriaTreeNode
     */
    private def createSubCriteria(def criteria, CriteriaTreeNode criteriaTreeNode) {
        criteriaTreeNode.children.each { associationName, childNode ->
            def joinType = childNode.joinType ?: CriteriaSpecification.INNER_JOIN
            Criteria childCriteria = criteria.createCriteria(childNode.name, childNode.alias, joinType)
            childNode.createdCriteria = childCriteria
            createSubCriteria(childCriteria, childNode)
        }
    }

    /**
     * Creates a HibernateCriteriaBuilder and runs the argument criteriaBuilder method.
     * @param method
     * @param additionalCriteria
     * @return
     */
    private def runQuery(String method, Closure additionalCriteria=null) {
        HibernateCriteriaBuilder criteriaBuilder = forClass.createCriteria()
        criteriaBuilder."$method" {
            if(additionalCriteria) {
                additionalCriteria.delegate = criteriaBuilder;
                additionalCriteria()
            }
            def criteriaInstance = criteriaBuilder.getInstance()
            createSubCriteria(criteriaInstance, criteriaTree)

            // Add the OR criteria.
            orCriteriaGroups.each { key, orList ->
                def disjunction = Restrictions.disjunction()
                orList.each {
                    disjunction.add(it)
                }
                criteriaInstance.add(disjunction)
            }

            // Add the AND criteria
            andCriteria.each {
                criteriaInstance.add(it)
            }
        }
    }

    /**
     * A node for the tree of association criteria and aliases.
     */
    private class CriteriaTreeNode {
        Map children = [:]
        int joinType
        Criteria createdCriteria
        String name
        String alias
    }

    /**
     * A custom SQLCriterion that performs a proper substitution of the {alias} placeholder.
     */
    private class AliasSQLCriterion extends SQLCriterion {
        def subCriteriaTreeNode

        /**
         * This constructor creates a new native SQL restriction with support for correct alias substitution.
         * @param sql The native SQL restriction. Use {alias} to reference the table name.
         * @param subCriteriaTreeNode The CriteriaTreeNode that contains the alias.
         */
        public AliasSQLCriterion(String sql, String search, CriteriaTreeNode subCriteriaTreeNode) {
            super(subCriteriaTreeNode.alias ? sql.replace("{alias}", subCriteriaTreeNode.alias) : sql, search, new StringType())
            this.subCriteriaTreeNode = subCriteriaTreeNode
        }

        /**
         * This is where we work the alias-substitution magic.
         */
        @Override
        public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
            // First replace the alias of the base table {alias}.
            def sql = super.toSqlString(criteria, criteriaQuery)
            def associationAlias = subCriteriaTreeNode.alias
            def queryAlias = criteriaQuery.getSQLAlias(subCriteriaTreeNode.createdCriteria)
            if(associationAlias && queryAlias) {
                sql = sql.replace(associationAlias, queryAlias)
            }
            sql
        }
    }
}