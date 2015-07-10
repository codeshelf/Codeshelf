/**
 * This is a sample Line extension point
 * Transforms the values of an order detail line before being parsed
 */
def OrderImportLineTransformation(orderLine) {
    orderLine.replace('~', ',');
}
