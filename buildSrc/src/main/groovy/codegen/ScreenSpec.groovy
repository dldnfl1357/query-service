package codegen

import org.yaml.snakeyaml.Yaml

class ScreenSpec {
    String screen           // camelCase, e.g. "orderList"
    String basePackage      // e.g. "com.example.queryservice"
    String table            // snake_case, e.g. "order_list_view"
    String primaryKeyField  // e.g. "orderId"
    String primaryKeyColumn // e.g. "order_id"
    String primaryKeyType   // e.g. "Long"
    List<Section> sections = []
    List<List<String>> indexes = []  // each entry = list of field names

    static ScreenSpec load(File file) {
        def raw = new Yaml().load(file.text) as Map
        def spec = new ScreenSpec()
        spec.screen = raw.screen
        spec.basePackage = raw.basePackage ?: 'com.example.queryservice'
        spec.table = raw.table ?: (toSnake(spec.screen) + '_view')
        spec.primaryKeyField = raw.primaryKey.field
        spec.primaryKeyColumn = raw.primaryKey.column ?: toSnake(spec.primaryKeyField)
        spec.primaryKeyType = raw.primaryKey.type ?: 'Long'

        raw.sections.each { Map s ->
            def section = new Section()
            section.name = s.name
            section.queueProperty = s.queue ?: "query-service.queues.${s.name}"
            section.matchByField = s.matchBy ?: spec.primaryKeyField
            section.isRoot = s.root == true
            section.fields = (s.fields ?: []).collect { Map f ->
                def field = new Field()
                field.name = f.name
                field.column = f.column ?: toSnake(f.name)
                field.type = f.type ?: 'String'
                field.length = f.length as Integer
                field.precision = f.precision as Integer
                field.scale = f.scale as Integer
                field
            }
            spec.sections << section
        }

        spec.indexes = (raw.indexes ?: []).collect { it.on as List<String> }
        spec
    }

    String screenPascal()  { capitalize(screen) }
    String screenSnake()   { toSnake(screen) }

    static String capitalize(String s) { s ? s[0].toUpperCase() + s.substring(1) : s }
    static String toSnake(String s)    { s.replaceAll(/([a-z0-9])([A-Z])/, '$1_$2').toLowerCase() }

    static class Section {
        String name             // camelCase, e.g. "member"
        String queueProperty    // SpEL placeholder body, e.g. "query-service.queues.member"
        String matchByField     // e.g. "memberId" or "orderId"
        boolean isRoot          // root section is upserted; others partial-update
        List<Field> fields = []

        String namePascal() { ScreenSpec.capitalize(name) }
        String sectionUpdatedAtField()  { "${name}SectionUpdatedAt" }
        String sectionUpdatedAtColumn() { "${name}_section_updated_at" }
        String matchByColumn()          { ScreenSpec.toSnake(matchByField) }
    }

    static class Field {
        String name
        String column
        String type      // Java simple type: Long, Integer, String, BigDecimal, Instant
        Integer length   // String
        Integer precision // BigDecimal
        Integer scale    // BigDecimal

        String jpaColumnAnnotation() {
            def attrs = ["name = \"${column}\""]
            if (type == 'String' && length) attrs << "length = ${length}"
            if (type == 'BigDecimal') {
                if (precision) attrs << "precision = ${precision}"
                if (scale != null) attrs << "scale = ${scale}"
            }
            "@Column(${attrs.join(', ')})"
        }

        String sqlType() {
            switch (type) {
                case 'Long':       return 'bigint'
                case 'Integer':    return 'int'
                case 'String':     return "varchar(${length ?: 255})"
                case 'BigDecimal': return "decimal(${precision ?: 19}, ${scale ?: 2})"
                case 'Instant':    return 'datetime(6)'
                default: throw new IllegalArgumentException("Unsupported type: ${type}")
            }
        }

        String javaImport() {
            switch (type) {
                case 'BigDecimal': return 'java.math.BigDecimal'
                case 'Instant':    return 'java.time.Instant'
                default: return null
            }
        }
    }
}
