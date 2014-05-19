import random

from django import template

register = template.Library()

@register.filter('field_class')
def field_class(field, cls):
    """
    Append class to field.
    """
    
    attrs = field.field.widget.attrs
    
    if not attrs.get('class', None):
        attrs['class'] = ''

    attrs['class'] += ' ' + cls

    return field

@register.filter
def placeholder(field, txt):
    """
    Add placeholder to form field.
    """
    
    attrs = field.field.widget.attrs
    
    attrs['placeholder'] = txt

    return field

@register.filter
def encode_str(value, encoding):
    return value.encode(encoding)

@register.filter
def decode_str(value, encoding):
    return value.decode(encoding)

@register.filter
def multiply(value, times):
    return value * times

@register.filter
def multiplyint(value, times):
    return int(value) * times

@register.filter('range')
def range_filter(num):
    return range(int(num) + 1)[1:]

@register.filter
def randompart(items):
    return random.choice(items.split(','))
