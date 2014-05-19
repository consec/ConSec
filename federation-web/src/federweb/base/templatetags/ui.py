from classytags.core import Options
from classytags.arguments import Argument
from classytags.helpers import Tag, InclusionTag

from django import template

register = template.Library()

@register.inclusion_tag('blocks/errors.html')
def form_errors(form):
    """
    Renders form errors using template blocks/errors.html
    
    Example::
    
        {% form_errors my_form %}
    
    """
    
    return dict(form=form)

@register.inclusion_tag('blocks/formfield.html')
def form_field(field, label='', cls='', extra=''):
    """
    Renders form field using template blocks/formfield.html.
    
    Example::
    
        {% form_fieled my_form.my_field 'My Field:' %}
    
    """
    
    return dict(field=field, label=label or field.label, field_class=cls, extra=extra)

class ListAction(InclusionTag):
    name = 'listaction'
    template = 'blocks/listaction.html'
    options = Options(
        blocks=[('endlistaction', 'nodelist')],
    )
    
    def get_context(self, context, nodelist):
        context.push()
        content = nodelist.render(context)
        context.pop()
        
        return {'content': content}

register.tag(ListAction)

@register.inclusion_tag('blocks/list_action.html')
def list_action(field, label='', cls='', extra=''):
    return dict(field=field, label=label or field.label, field_class=cls, extra=extra)

class Breadcrumb(Tag):
    options = Options(
        Argument('title'),
        Argument('link', required=False),
    )

    def render_tag(self, context, title, link):
        context['breadcrumbs'].insert(0, dict(title=title, link=link))
        return ''

register.tag(Breadcrumb)