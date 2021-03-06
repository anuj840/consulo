package com.intellij.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.util.IncorrectOperationException;
import javax.annotation.Nonnull;

/**
 * @author traff
 */
public class PsiReferenceWrapper implements PsiReference {
  private final PsiReference myOriginalPsiReference;

  public PsiReferenceWrapper(PsiReference originalPsiReference) {
    myOriginalPsiReference = originalPsiReference;
  }

  @Override
  public PsiElement getElement() {
    return myOriginalPsiReference.getElement();
  }

  @Override
  public TextRange getRangeInElement() {
    return myOriginalPsiReference.getRangeInElement();
  }

  @Override
  public PsiElement resolve() {
    return myOriginalPsiReference.resolve();
  }

  @Nonnull
  @Override
  public String getCanonicalText() {
    return myOriginalPsiReference.getCanonicalText();
  }

  @Override
  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
    return myOriginalPsiReference.handleElementRename(newElementName);
  }

  @Override
  public PsiElement bindToElement(@Nonnull PsiElement element) throws IncorrectOperationException {
    return myOriginalPsiReference.bindToElement(element);
  }

  @Override
  public boolean isReferenceTo(PsiElement element) {
    return myOriginalPsiReference.isReferenceTo(element);
  }

  @Nonnull
  @Override
  public Object[] getVariants() {
    return myOriginalPsiReference.getVariants();
  }

  @Override
  public boolean isSoft() {
    return myOriginalPsiReference.isSoft();
  }

  public <T extends PsiReference> boolean isInstance(Class<T> clazz) {
    if (myOriginalPsiReference instanceof PsiReferenceWrapper) {
      return ((PsiReferenceWrapper)myOriginalPsiReference).isInstance(clazz);
    }
    return clazz.isInstance(myOriginalPsiReference);
  }

  public <T extends PsiReference> T cast(Class<T> clazz) {
    if (myOriginalPsiReference instanceof PsiReferenceWrapper) {
      return ((PsiReferenceWrapper)myOriginalPsiReference).cast(clazz);
    }
    return clazz.cast(myOriginalPsiReference);
  }
}
